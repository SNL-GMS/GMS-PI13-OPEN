{{/*
Render the docker image tag, truncated to 62 characters to match Gitlab's $CI_COMMIT_REF_SLUG.
Usage:
    {{ include "soh.imageTag" . }}
*/}}
{{- define "soh.imageTag" -}}
{{ .Values.imageTag | trunc 63 }}
{{- end -}}



{{/*
Render the full docker image path.
Usage:
    image: {{ include "soh.deployment.image" (dict "Values" .Values "appValues" $appValues) }}
Or:
    image: {{ include "soh.deployment.image" (dict "Values" .Values "appValues" (dict "imageName" "gms-common/centos")) }}
*/}}
{{- define "soh.deployment.image" -}}
  {{- if contains ":" .appValues.imageName -}}
    {{ printf "%s/%s" .Values.imageRegistry .appValues.imageName | quote }}
  {{- else -}}
    {{ printf "%s/%s:%s" .Values.imageRegistry .appValues.imageName (include "soh.imageTag" .) | quote }}
  {{- end -}}
{{- end -}}



{{/*
Render the app environment variables for a app's container by combining the
top-level `env` and the app's `env` from values.yaml (per-app envs take
precedence), and expanding templates in the resulting values.  A special
`.appName` is added to the context in the template expansion so that an app's
`env` can use `.appName` to refer to its own name.
Usage:
          env: {{- include "soh.app.deployment.env" (dict "appName" $appName "appEnv" $appValues.env "globalEnv" .Values.env "context" $) }}
*/}}
{{- define "soh.app.deployment.env" -}}
  {{- $mergedEnv := merge dict (.appEnv | default dict) (.globalEnv | default dict) -}}
  {{- if $mergedEnv -}}
    {{- range $key, $val := $mergedEnv }}
            - name: {{ $key | quote }}
              value: {{ tpl ($val | toString) (merge (dict "appName" $.appName) $.context) | quote }}
    {{- end }}
  {{- end -}}
{{- end -}}



{{/*
Render the app's Deployment strategy type.
Usage:
    strategy:
      type: {{ include "soh.deployment.strategy" (dict "Values" .Values "appValues" $appValues) }}
*/}}
{{- define "soh.deployment.strategy" -}}
{{ .appValues.deploymentStrategy | default "RollingUpdate" }}
{{- end -}}



{{/*
Compile all Values errors into a single message and call fail.
*/}}
{{- define "soh.validateValues" -}}
{{- $messages := list -}}
{{- $messages := empty .Values.imageRegistry | ternary "- imageRegistry value must be provided specifying the container image registry (e.g., '--set imageRegistry=docker-registry.example.com')." "" | append $messages -}}
{{- $messages := empty .Values.baseDomain | ternary "- baseDomain value must be provided specifying the base name for hostname-based Ingress routing (e.g., '--set baseDomain=cluster.example.com')." "" | append $messages -}}
{{- $messages := empty .Values.password.etcd.gms | ternary "- A password must be provided for the etcd gms user." "" | append $messages -}}
{{- $messages := empty .Values.password.etcd.gmsadmin | ternary "- A password must be provided for the etcd gmsadmin user." "" | append $messages -}}
{{- $messages := empty .Values.password.etcd.root | ternary "- A password must be provided for the etcd root user." "" | append $messages -}}
{{- $messages := empty .Values.password.postgres.gms_super_user | ternary "- A password must be provided for the postgres gms_super_user user." "" | append $messages -}}
{{- $messages := empty .Values.password.postgres.gms_admin | ternary "- A password must be provided for the postgres gms_admin user." "" | append $messages -}}
{{- $messages := empty .Values.password.postgres.gms_read_only | ternary "- A password must be provided for the postgres gms_read_only user." "" | append $messages -}}
{{- $messages := empty .Values.password.postgres.gms_config_application | ternary "- A password must be provided for the postgres gms_config_application user." "" | append $messages -}}
{{- $messages := empty .Values.password.postgres.gms_session_application | ternary "- A password must be provided for the postgres gms_session_application user." "" | append $messages -}}
{{- $messages := empty .Values.password.postgres.gms_soh_application | ternary "- A password must be provided for the postgres gms_soh_application user." "" | append $messages -}}
{{- $messages := empty .Values.password.postgres.gms_soh_test_application | ternary "- A password must be provided for the postgres gms_soh_test_application user." "" | append $messages -}}
{{- $messages := empty .Values.password.postgres.gms_soh_ttl_application | ternary "- A password must be provided for the postgres gms_soh_ttl_application user." "" | append $messages -}}
{{- $messages := eq .Release.Name "default" | ternary "- The instance name cannot be 'default'." "" | append $messages -}}
{{- $messages := ne .Release.Name .Release.Namespace | ternary (printf "- The instance name (%s) and the namespace name (%s) must match (e.g., '--namespace %s --create-namespace')." .Release.Name .Release.Namespace .Release.Name) "" | append $messages -}}
{{- $messages := and .Values.injector .Values.liveData | ternary "- You cannot use both injector and liveData at the same time." "" | append $messages -}}
{{- $messages := without $messages "" -}}
{{- if $messages -}}
  {{- printf "\nVALUES VALIDATION ERRORS:\n%s" (join "\n" $messages) | fail -}}
{{- end -}}
{{- end -}}
