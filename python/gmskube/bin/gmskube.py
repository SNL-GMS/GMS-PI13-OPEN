#!/usr/bin/env python3

# --------------------------------------------------------------------
#  gmskube - Geophysical Monitoring System Control Utility
#
#  The gmskube command-line program is used to install and configure
#  instances of the GMS (Geophysical Monitoring System) system
#  on Kubernetes.
# --------------------------------------------------------------------
import argparse
import base64
import getpass
import io
import json
import os
import re
import sys
import tarfile
import time
import traceback
from argparse import ArgumentParser, RawDescriptionHelpFormatter
from signal import signal, SIGINT

import requests
from requests.adapters import HTTPAdapter
from requests.packages.urllib3.util.retry import Retry

from gmsutils import logging, print_color, tc, print_error, print_warning, run_command

# Types
TYPES = ['soh', 'ian', 'logging', 'grafana', 'sb']

def main():
    (args, parser) = get_args()

    # configure logging - make sure this comes before any call to logging
    # remove any existing logging handlers that may have been setup in imports
    while len(logging.root.handlers):
        logging.root.removeHandler(logging.root.handlers[-1])
    logging.basicConfig(format='[%(levelname)s] %(message)s', level=getattr(logging, args.verbose))
    # capture any messages from the warnings module
    logging.captureWarnings(True)

    # save kubectl context into a file if the env var is set
    if 'KUBECTL_CONTEXT' in os.environ:
        logging.debug('KUBECTL_CONTEXT is set, saving file')
        with open(os.getenv('KUBECONFIG'), "w") as kube_file:
            print(f"{os.getenv('KUBECTL_CONTEXT')}", file=kube_file)

    # set SSL cert path for python requests library
    if 'REQUESTS_CA_BUNDLE' not in os.environ:
        # this path is the default for centos7
        os.environ['REQUESTS_CA_BUNDLE'] = '/etc/pki/tls/certs/ca-bundle.crt'

    # print debug arguments
    logging.debug('Arguments:')
    for arg in vars(args):
        logging.debug(f"  {arg} = {getattr(args, arg) or ''}")

    # print out the entire env for debug
    logging.debug('Environment:')
    logging.debug('\n'.join([f'{key}={value}' for key, value in sorted(os.environ.items())]))

    # call appropriate function if a command was specified, otherwise just print help.
    if hasattr(args, 'command'):
        try:
            args.command(args)
        except Exception as ex:
            print_error(ex)
            traceback.print_exc()
            sys.exit(1)
    else:
        help_command(args, parser)


def get_args():
    # Get command-line arguments.

    description = """
description:
  The gmskube command-line program is used to install and configure instances
  of the GMS (Geophysical Monitoring System) system on Kubernetes.

  Each "instance" is an install of a multi-container application that is
  managed as a single unit and runs on a Kubernetes cluster. Each instance is
  contained within its own namespace in Kubernetes.  Various predefined types
  of instances are available.

  Some example instance types would be 'soh', 'ian', 'logging', 'sb' or 'grafana'.

  Multiple copies of 'soh' type instance may be run simultaneously. Each
  instance must be given a unique name to identify it as well as distinguish it
  from other running instances of the same type.

  For example, one instance of 'soh' may be running as 'develop' while another
  instance of 'soh' may be running as 'integration'.

  Different versions of a instance type may be available from the configured
  Docker registry. Released versions of GMS are tagged with a specific version
  number. During development this would correspond to a tag name on the docker images.

configuration:
  Before you can run gmskube, you must first download a Kubeconfig bundle from
  the cluster, and have the kubectl context set to the correct cluster.

  1. Login to Rancher
  2. Click the cluster name
  3. In the upper right, click the blue Kubeconfig File button
  4. Copy/Paste the contents into ~/.kube/config on your development machine
  5. If you have kubectl installed, the KUBECONFIG environment variable should
     already be set.  If not, set KUBECONFIG=~/config

commands:
  See the --help for details of each command.
   
examples:
  Get usage help for the gmskube tool:
    $ gmskube --help
    
  Install a SOH deployment of the default tag, with name 'my-test':
    $ gmskube install --type soh my-test
    
  Install a SOH deployment of the tag 'tag123', with the name 'my-test':
    $ gmskube install --type soh --tag tag123 my-test
"""
    parser = ArgumentParser(description=description,
                            formatter_class=RawDescriptionHelpFormatter)

    # global arguments
    parser.add_argument('-v', '--verbose', default='INFO', action='store_const', const='DEBUG',
                        help='Enable debug level output.')
    parser.add_argument('--timeout', type=int, default=4,
                        help='Specify the max time in minutes (integer) that should be waited when services are '
                             'scaled up or down.')

    # Parent parsers contains common arguments that can be reused when adding a parser
    # Only add a parent parser if it will be used in more than one command. Otherwise
    # just add it directly to the command.

    # parent name parser
    parent_name_parser = argparse.ArgumentParser(add_help=False)
    parent_name_parser.add_argument('name', nargs=1, type=argparse_instance_name_type,
                                    help='Name of the instance')

    # parent tag parser
    parent_tag_parser = argparse.ArgumentParser(add_help=False)
    parent_tag_parser.add_argument('--tag', required=True, type=argparse_tag_name_type,
                                   help='Tag name, which corresponds to the docker tag of the images. '
                                        'The value entered will automatically be transformed according to the '
                                        'definition of the gitlab CI_COMMIT_REF_SLUG variable definition '
                                        '(lowercase, shortened to 63 characters, and with everything except '
                                        '`0-9` and `a-z` replaced with `-`, no leading / trailing `-`).')

    # parent set parser
    parent_set_parser = argparse.ArgumentParser(add_help=False)
    parent_set_parser.add_argument('--set', dest='namevalue', type=argparse_set_type, action='append',
                                   help='Set a value in the chart to the specified value.  May be specified '
                                        'multiple times for different values.  Examples: `--set foo=bar` to '
                                        'set value `foo` to `bar`.  `--set env.GLOBAL_VAR=Hello` to set the '
                                        '`GLOBAL_VAR` environment variable to `Hello` in all application Pods '
                                        'within the instance.  `--set cd11-connman.env.CONNMAN_VAR=World` to '
                                        'set the `CONNMAN_VAR` environment var to `World` only in the '
                                        '`cd11-connman` app\'s Pod. `--set bastion.replicas=0` to set the '
                                        '`replicas` chart value in the bastion chart to `0`.')

    # parent injector livedata parser
    parent_injector_livedata_parser = argparse.ArgumentParser(add_help=False)
    # mutual exclusive group for injector and livedata
    injector_livedata_group = parent_injector_livedata_parser.add_mutually_exclusive_group()
    injector_livedata_group.add_argument('--injector', default=False, action='store_true',
                                         help='Include the data injector in the instance')
    injector_livedata_group.add_argument('--livedata', default=False, action='store_true',
                                         help='Include live data in the instance')
    # optional args for injector and live data
    parent_injector_livedata_parser.add_argument('--injector-dataset',
                                                 help='Dataset for the injector. If not specified, the default is '
                                                      'the value set in the helm "values.yaml" file.')
    parent_injector_livedata_parser.add_argument('--connman-port', type=int,
                                                 help='If specified, sets the environment variable to change the '
                                                      'well known port for the CD11 connman service, and configures '
                                                      'the port in kubernetes to be externally visible.')
    parent_injector_livedata_parser.add_argument('--connman-data-manager-ip', type=argparse_ip_address_type,
                                                 help='If specified, sets the environment variable to change the '
                                                      'external IP address of the CD11 dataman service.')
    parent_injector_livedata_parser.add_argument('--connman-data-provider-ip', type=argparse_ip_address_type,
                                                 help='If specified, sets the environment variable to change IP '
                                                      'address of the data provider sending data to the CD11 dataman '
                                                      'service.')
    parent_injector_livedata_parser.add_argument('--dataman-ports', type=argparse_dataman_ports_type,
                                                 help='If specified, sets the environment variable to change the port '
                                                      'range for the CD11 dataman service, and configures the ports '
                                                      'in kubernetes to be externally visible.')

    # parent config parser
    parent_config_parser = argparse.ArgumentParser(add_help=False)
    parent_config_parser.add_argument('--config', default=None, type=str,
                                      help='Path to a directory of configuration overrides to load into instance')

    subparsers = parser.add_subparsers(help='Available sub-commands:')

    # Install
    install_parser = subparsers.add_parser('install',
                                           parents=[parent_name_parser, parent_tag_parser,
                                                    parent_set_parser, parent_injector_livedata_parser,
                                                    parent_config_parser],
                                           help='Install an instance of the system')
    # type and chart are mutually exclusive, and at least one must be specified. Chart arg gets repeated again in
    # upgrade, but we can't implement as a parent parser due to the group here.
    install_type_chart_group = install_parser.add_mutually_exclusive_group(required=True)
    install_type_chart_group.add_argument('--type', choices=TYPES,
                                          help='Type of instance')
    install_type_chart_group.add_argument('--chart', default=None, type=str,
                                          help='Path to a local helm chart directory to deploy. If not specified, '
                                               'the helm chart is automatically extracted from a docker image that '
                                               'contains the chart files for the branch. Note the directory must '
                                               'exist at or below the present directory (PWD), no `../` is allowed.')
    install_parser.add_argument('--oracle-wallet', default=None, type=str,
                                          help='Path to an Oracle Wallet directory. The contained files will be base64 '
                                               'encoded and made available to the chart in the "oracle-wallet" value.')

    install_parser.set_defaults(command=install_command)

    # Reconfig
    reconfig_parser = subparsers.add_parser('reconfig',
                                            parents=[parent_name_parser, parent_config_parser, parent_tag_parser],
                                            help='Reconfigure a running instance of a system')
    reconfig_parser.set_defaults(command=reconfig_command)

    # Upgrade
    upgrade_parser = subparsers.add_parser('upgrade',
                                           parents=[parent_name_parser, parent_set_parser,
                                                    parent_injector_livedata_parser, parent_tag_parser],
                                           help='Upgrade an instance of the system')
    # for Upgrade, type is not an option since we don't want to let people change the type during an upgrade. Chart
    # is optional here. Not implemented as a parent parser since it won't work with the group in Install.
    upgrade_parser.add_argument('--chart', default=None, type=str,
                                help='Path to a local helm chart directory to deploy. If not specified, '
                                     'the helm chart is automatically extracted from a docker image that '
                                     'contains the chart files for the branch. Note the directory must '
                                     'exist at or below the present directory (PWD), no `../` is allowed.')
    upgrade_parser.set_defaults(command=upgrade_command)

    # Uninstall
    uninstall_parser = subparsers.add_parser('uninstall',
                                             parents=[parent_name_parser],
                                             help='Uninstall an instance of the system')
    uninstall_parser.set_defaults(command=uninstall_command)

    # List
    list_parser = subparsers.add_parser('list', aliases=['ls'], help='List instances')
    list_parser.set_defaults(command=list_command)

    args = parser.parse_args()

    # check if livedata is specified for any optional live data args
    if (getattr(args, 'connman_port', None) is not None
        or getattr(args, 'connman_data_manager_ip', None) is not None
        or getattr(args, 'connman_data_provider_ip', None) is not None
        or getattr(args, 'dataman_ports', None) is not None) and not getattr(args, 'livedata', False):
        parser.error('--livedata must be specified if any of --connman-port, --connman-data-manager-ip, '
                     '--connman-data-provider-ip, or --dataman-ports are provided.')

    return args, parser


def argparse_instance_name_type(s, pat=re.compile(r'^[a-z0-9][a-z0-9-]{1,126}[a-z0-9]$')):
    """
    Define an argparse type for instance names.  This checks two limitations
    that apply to instance names:
    1. Instance name length is between 3 and 128 characters. Until we find out
       otherwise, this is an arbitrary limit.
    2. The instance name will be used as part of a DNS hostname, so it must
       comply with DNS naming rules:
       "hostname labels may contain only the ASCII letters 'a' through 'z' (in
       a case-insensitive manner), the digits '0' through '9', and the hyphen
       ('-'). The original specification of hostnames in RFC 952, mandated that
       labels could not start with a digit or with a hyphen, and must not end
       with a hyphen. However, a subsequent specification (RFC 1123) permitted
       hostname labels to start with digits. No other symbols, punctuation
       characters, or white space are permitted.
    """

    if not pat.match(s):
        raise argparse.ArgumentTypeError(
            'Instance name must be between 3 and 128 characters long, consist only of lower case letters '
            'digits, and hyphens.')
    return s


def argparse_tag_name_type(s):
    """
    Transform the tag name into the CI_COMMIT_REF_SLUG as defined by gitlab:
    Lower-cased, shortened to 63 bytes, and with everything except `0-9` and `a-z` replaced with `-`.
    No leading / trailing `-`
    """

    # s.lower() changes to lower case, re.sub replaces anything other than a-z 0-9 with `-`
    # strip('-') removes any leading or trailing `-` after re.sub, finally [:63] truncates to 63 chars
    return re.sub(r'[^a-z0-9]', '-', s.lower()).strip('-')[:63]


def argparse_set_type(s, pat=re.compile(r'^.+=.*')):
    """
    Use a regular expression to match a helm value of the form:
    VARIABLE=VALUE
    Helm accepts a lot of different values, https://helm.sh/docs/intro/using_helm/#the-format-and-limitations-of---set
    so the regex is not very restrictive to allow for all the different forms
    """
    m = pat.match(s)

    if not m:
        raise argparse.ArgumentTypeError(
            'When specifying `--set`, you must supply helm chart name/value pair as: `Name=Value`'
        )

    return s


def argparse_ip_address_type(s, pat=re.compile(r'^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$')):
    """
    Really basic IP address validation
    """
    if not pat.match(s):
        raise argparse.ArgumentTypeError(
            'Must be a valid IP address (example: 192.168.1.1)'  #NOSONAR - just an example; does not need to be configurable
        )
    return s


def argparse_dataman_ports_type(s, pat=re.compile(r'^\d{1,5}-\d{1,5}$')):
    """
    Dataman port range should be two numbers separated by a dash
    """

    if not pat.match(s):
        raise argparse.ArgumentTypeError(
            'Dataman port range must be two integers separated by a dash (example: 8100-8199)'
        )
    return s


def install_command(args, parser=None):
    """
    Perform helm install command, with some extra options for data load
    """

    kubectl_host = os.getenv('KUBECTL_HOST')
    docker_registry = os.getenv('CI_DOCKER_REGISTRY')

    # Grab any custom helm values set by the --set option
    set_string = ""
    if args.namevalue is not None:
        for item in args.namevalue:
            set_string += f'--set "{item}" '

    # add injector/livedata args
    set_string += get_injector_livedata_set_args(args.injector, args.injector_dataset, args.livedata,
                                                 args.connman_port, args.connman_data_manager_ip,
                                                 args.connman_data_provider_ip, args.dataman_ports)

    # add Oracle Wallet args
    set_string += get_oracle_wallet_set_args(args.oracle_wallet)

    # Set the instance type to be custom if loading in a user-defined custom chart directory
    if args.chart is not None:
        args.type = "custom"

    # run helm install, change into the deploy directory first
    # Deploy directory is /deploy in the docker image, but can be overridden for development by setting env var
    return_code, out, err = run_command(
        f'helm install {args.name[0]} {args.type} '
        f'--namespace {args.name[0]} '
        f'--create-namespace '
        f'--set "baseDomain={kubectl_host}" '
        f'--set "kafka.clusterDomain={kubectl_host}" '
        f'--set "imageRegistry={docker_registry}" '
        f'--set "global.imageRegistry={docker_registry}" '
        f'--set "kafka.image.registry={docker_registry}" '
        f'--set "kafka.zookeeper.image.registry={docker_registry}" '
        f'--set "imageTag={args.tag}" '
        f'--set "global.imageTag={args.tag}" '
        f'--set "kafka.image.tag={args.tag}" '
        f'--set "kafka.zookeeper.image.tag={args.tag}" '
        f'--set "user={getpass.getuser()}" '
        f'--set "password.etcd.gms=gmsdb:etcd:gms:packager-tungsten-sort" '
        f'--set "password.etcd.gmsadmin=gmsdb:etcd:gmsadmin:nonsaturated-southerns-souls" '
        f'--set "password.etcd.root=gmsdb:etcd:root:biophysicist-grandbaby-countermen" '
        f'--set "password.postgres.gms_super_user=gmsdb:postgres:gms_super_user:responded-outcrops-eighteen" '
        f'--set "password.postgres.gms_admin=gmsdb:postgres:gms_admin:over-realized-exclusivism" '
        f'--set "password.postgres.gms_config_application=gmsdb:postgres:gms_config_application:good-steel-referees" '
        f'--set "password.postgres.gms_read_only=gmsdb:postgres:gms_read_only:humoured-tempered-furious-lion" '
        f'--set "password.postgres.gms_session_application=gmsdb:postgres:gms_session_application:thawed-mortals-rebounds" '
        f'--set "password.postgres.gms_soh_application=gmsdb:postgres:gms_soh_application:smidgeons-offerers-reducers" '
        f'--set "password.postgres.gms_soh_test_application=gmsdb:postgres:gms_soh_test:productive-dominancy-cordlessly" '
        f'--set "password.postgres.gms_soh_ttl_application=gmsdb:postgres:gms_soh_ttl_application:ceilings-originate-eleventh" '
        # These --set commands are for GRAFANA --BEGIN
        f'--set "image.repository={docker_registry}/gms-common/monitoring-grafana" '
        f'--set "image.tag={args.tag}" '
        f'--set "initChownData.image.repository={docker_registry}/gms-common/monitoring-busybox" '
        f'--set "initChownData.image.tag={args.tag}" '
        # These --set commands are for GRAFANA --END
        # These --set commands are for LOGGING --BEGIN
        f'--set "global.baseDomain={kubectl_host}" '
        f'--set "ldap-proxy.imageTag={args.tag}" '
        f'--set "ldap-proxy.imageName=gms-common/ldap_proxy" '
        f'--set "ldap-proxy.baseDomain={kubectl_host}" '
        f'--set "ldap-proxy.imageRegistry={docker_registry}" '
        f'--set "elasticsearch.image={docker_registry}/gms-common/logging-elasticsearch" '
        f'--set "elasticsearch.imageTag={args.tag}" '
        f'--set "fluent-bit.image.fluent_bit.repository={docker_registry}/gms-common/logging-fluent-bit" '
        f'--set "fluent-bit.image.fluent_bit.tag={args.tag}" '
        f'--set "fluent-bit.initContainers.wait-for-es-config.image={docker_registry}/gms-common/centos" '
        f'--set "kibana.image={docker_registry}/gms-common/logging-kibana" '
        f'--set "kibana.imageTag={args.tag}" '
        # These --set commands are for LOGGING --END
        f'--render-subchart-notes '
        f'{set_string}',
        chdir=os.getenv('DEPLOY_DIR', '/deploy'),
        print_command=(args.verbose == 'DEBUG'),
        print_output=True)

    if return_code > 0:
        print_error(f'Could not install instance {args.name[0]}: {err}')
        exit(return_code)

    # Run the config-loader
    if not request_dataload(f'{kubectl_host}', f'{args.name[0]}', config_overrides=args.config,
                            timeout=args.timeout):
        print_error(f'Dataload failed to execute successfully...Exiting')
        exit(1)

    print_color(tc.GREEN, f'{args.name[0]} installed successfully!', bold=True)
    print('\nBelow are the ingress routes for each service in the instance:')
    # print ingress routes
    run_command(f'kubectl get ingress '
                f'--namespace {args.name[0]} '
                f'--output custom-columns=SERVICE:.metadata.name,INGRESS:.spec.rules[*].host',
                print_command=(args.verbose == 'DEBUG'),
                print_output=True)


def upgrade_command(args, parser=None):
    """
    Perform helm upgrade command
    """

    # Grab any custom helm values set by the --set option
    set_string = ""
    if args.namevalue is not None:
        for item in args.namevalue:
            set_string += f'--set "{item}" '

    # add injector/livedata args
    set_string += get_injector_livedata_set_args(args.injector, args.injector_dataset, args.livedata,
                                                 args.connman_port, args.connman_data_manager_ip,
                                                 args.connman_data_provider_ip, args.dataman_ports)

    if args.chart is not None:
        # Set the instance type to be custom if loading in a user-defined custom chart directory
        instance_type = "custom"
    else:
        # get the instance type from the labels. We don't use args.type here because we don't allow the type
        # to be changed during upgrade.
        instance_type = get_instance_labels(args.name[0])['gms/type']

    # run helm upgrade, use --reuse-values, change into the deploy directory first
    # Deploy directory is /deploy in the docker image, but can be overridden for development by setting env var
    return_code, out, err = run_command(
        f'helm upgrade {args.name[0]} {instance_type} '
        f'--namespace {args.name[0]} '
        f'--reuse-values '
        f'--set "imageTag={args.tag}" '
        f'--render-subchart-notes '
        f'{set_string}',
        chdir=os.getenv('DEPLOY_DIR', '/deploy'),
        print_command=(args.verbose == 'DEBUG'),
        print_output=True)

    if return_code > 0:
        print_error(f'Could not upgrade instance {args.name[0]}: {err}')
        exit(return_code)


def uninstall_command(args, parser=None):
    """
    Perform helm uninstall command
    """
    # run helm uninstall
    return_code, out, err = run_command(
        f'helm uninstall {args.name[0]} '
        f'--namespace {args.name[0]}',
        print_command=(args.verbose == 'DEBUG'),
        print_output=True)

    if return_code > 0:
        print_error(f'Could not uninstall instance {args.name[0]}: {err}')
        exit(return_code)


def request_dataload(hostname, instance_name, endpoint='load', config_overrides=None, timeout=4):
    timeout_seconds = timeout * 60

    # check if config-loader service exists in the instance
    return_code, out, err = run_command(f'kubectl get service config-loader --namespace {instance_name}',
                                        print_output=False)
    if return_code > 0:
        logging.debug('config-loader service does not exist, skipping dataload')
        return True

    try:
        retry_strategy = Retry(total=20, backoff_factor=0.2, status_forcelist=[404], method_whitelist=["POST", "GET"])
        adapter = HTTPAdapter(max_retries=retry_strategy)
        http = requests.Session()
        http.mount("https://", adapter)

        # must be https on kube cluster, and note the requests CA bundle env var must be set
        config_loader_url = f"https://config-loader-{instance_name}.{hostname}"

        if config_overrides:
            override_file = get_override_tar_file(config_overrides)
            if not override_file:
                print_error('Unable to create tar file from user supplied overrides')
                exit(1)
            files = {'files': override_file}
        else:
            files = None

        print_color(tc.MAGENTA, 'Waiting for config loader to be alive...')
        time_waited = 0
        while time_waited < timeout_seconds:
            post_response = http.get(f"{config_loader_url}/alive")

            if post_response.status_code == 200:
                break

            if time_waited % 30 == 0:  # print a message every 30 seconds noting that we are waiting
                print_color(tc.MAGENTA, 'Waiting for config loader to be alive...')

            time.sleep(1)
            time_waited += 1

            if time_waited >= timeout_seconds:
                print_warning('Timed out waiting for config loader to be alive, will attempt dataload anyway')

        print_color(tc.MAGENTA, 'Requesting dataload...')
        post_response = http.post(f"{config_loader_url}/{endpoint}", files=files)

        if post_response.status_code != 200:
            print_error(f'Failed to initiate a data load. {post_response.status_code}: {post_response.reason}')
            exit(1)

        # Wait for results from the config-loader service
        time_waited = 0
        while time_waited < timeout_seconds:
            result_response = json.loads(http.get(f"{config_loader_url}/result").text)
            if result_response['status'] == 'FINISHED':
                break

            if time_waited % 15 == 0:  # print a message every 15 seconds noting that we are waiting
                print_color(tc.MAGENTA, 'Waiting for data load to complete...')

            time.sleep(1)
            time_waited += 1

        if result_response['status'] != 'FINISHED':
            print_error(f'Timed out waiting for dataload after {timeout} minutes...Exiting')
            exit(1)

        if result_response['successful']:
            print(result_response['result'])
            print_color(tc.MAGENTA, 'Data load successfully completed.')
            return True
        else:
            print(result_response['result'])
            print_error(f'Dataload failed to execute successfully...Exiting')
            exit(1)

    except Exception as e:
        print_error(e)
        exit(1)


def get_override_tar_file(config_dir):
    try:
        buffered_tarfile = None

        # This method will take the input config dir and create a tar file
        rootDir = config_dir
        filelist = []
        dirlist = [f"{rootDir}/processing", f"{rootDir}/station-reference/stationdata", f"{rootDir}/user-preferences"]

        for overrideDir in dirlist:
            if os.path.exists(overrideDir):
                for root, dirs, files in os.walk(overrideDir):
                    for name in files:
                        fullpathfilename = os.path.join(root, name)
                        subpathfilename = os.path.relpath(fullpathfilename, rootDir)
                        filelist.append(subpathfilename)

        # Get the current dir so we can switch back at the end
        start_dir = os.getcwd()

        # Change to the config override directory
        os.chdir(rootDir)

        # Create the tar file
        fh = io.BytesIO()
        with tarfile.open(fileobj=fh, mode='w:gz') as tar:
            for file in filelist:
                # ignore any filenames that start with '.'
                if not file.startswith('.'):
                    tar.add(file)

        buffered_tarfile = fh.getbuffer()

    except Exception as ex:
        print_error(f'{ex.explanation}')

    finally:
        return buffered_tarfile


def reconfig_command(args, parser=None):
    """
    Perform the instance reconfig command - stop all dataload services, run a reduced dataload, and restart services
    """

    # get the instance type from the labels
    instance_type = get_instance_labels(args.name[0])['gms/type']

    print_color(tc.CYAN, f'Stopping application services in instance: {args.name[0]} ...')
    return_code, out, err = run_command(
        f'helm upgrade {args.name[0]} {instance_type} '
        f'--namespace {args.name[0]} '
        f'--reuse-values '
        f'--set "imageTag={args.tag}" '
        f'--render-subchart-notes '
        f'--set "reconfigInProgress=1"',
        chdir=os.getenv('DEPLOY_DIR', '/deploy'),
        print_command=(args.verbose == 'DEBUG'),
        print_output=True)

    if return_code > 0:
        print_error(f'Dataload services failed to scale down...Exiting')
        exit(return_code)

    if not request_dataload(os.getenv('KUBECTL_HOST'), f'{args.name[0]}', endpoint='reload',
                            config_overrides=args.config,
                            timeout=args.timeout):
        print_error(f'Dataload failed to execute successfully...Exiting')
        exit(1)

    print_color(tc.CYAN, f'Starting application services in instance: {args.name[0]} ...')
    return_code, out, err = run_command(
        f'helm upgrade {args.name[0]} {instance_type} '
        f'--namespace {args.name[0]} '
        f'--reuse-values '
        f'--render-subchart-notes '
        f'--set "reconfigInProgress=0"',
        chdir=os.getenv('DEPLOY_DIR', '/deploy'),
        print_command=(args.verbose == 'DEBUG'),
        print_output=True)

    if return_code > 0:
        print_error(f'Dataload services failed to scale up...Exiting')
        exit(return_code)


def list_command(args, parser=None):
    '''
    List helm instances and show some gms labels
    '''

    # Get all the helm instances
    return_code, out, err = run_command('helm list --all --all-namespaces --output json', print_output=False)
    if return_code > 0:
        print_error(f'Could not list instances: {err}')
        print_command=(args.verbose == 'DEBUG'),
        exit(return_code)

    # column format
    col_format = '%-32s   %-10s   %-8s   %-13s   %-18s   %-14s   %-23s'
    # Setup the header
    print(col_format % ('NAME',
                        'STATUS',
                        'TYPE',
                        'USER',
                        'UPDATED',
                        'CD11-PORTS',
                        'TAG',
                        ))
    print(col_format % ('----',
                        '------',
                        '----',
                        '----',
                        '-------',
                        '----------',
                        '---',
                        ))

    instances = json.loads(out)
    for item in instances:
        # get labels for each instance
        # TODO: iterating through each instance may not be the most efficient way
        labels = get_instance_labels(item['name'])

        # Only display something in the CD11-PORTS for instances attached to live data
        livedata = "-"
        if labels.get('gms/cd11-live-data', '') == 'true' \
           and labels.get('gms/cd11-connman-port') \
           and labels.get('gms/cd11-dataman-port-start') \
           and labels.get('gms/cd11-dataman-port-end'):
            livedata = '%s,%s-%s' % (labels.get('gms/cd11-connman-port'),
                                     labels.get('gms/cd11-dataman-port-start'),
                                     labels.get('gms/cd11-dataman-port-end')
                                     )

        print(col_format % (item['name'],
                            item['status'],
                            labels.get('gms/type', '?'),
                            labels.get('gms/user', '?'),
                            labels.get('gms/update-time', '?'),
                            livedata,
                            labels.get('gms/image-tag', '?'),
                            ))


def get_instance_labels(name):
    """
    Gets the gms labels for an instance
    :param name: Name of the instance
    :return: Dictionary with gms key value pairs representing the labels
    """
    return_code, out, err = run_command(
        f'kubectl get configmap --namespace {name} --field-selector metadata.name==gms --show-labels --no-headers',
        print_output=False)

    try:
        labels = dict(item.split("=") for item in out.split()[3].split(","))
    except Exception as ex:
        logging.debug(f'Error splitting labels for configmap gms in namespace {name}: {ex}')
        labels = {}

    logging.debug(f'Labels for ConfigMap "gms" in Namespace "{name}"')
    logging.debug(labels)

    return labels


def get_injector_livedata_set_args(injector, injector_dataset, livedata, connman_port, connman_data_manager_ip,
                                   connman_data_provider_ip, dataman_ports):
    """
    Returns a string containing set arguments needed for the injector or live data
    :param injector: boolean indicating if the injector should be enabled
    :param injector_dataset: name of the dataset for the injector
    :param livedata: boolean indicating if live data should be enabled
    :param connman_port: well known port for CD11 connman service
    :param connman_data_manager_ip: external IP address of the CD11 dataman service
    :param connman_data_provider_ip: IP address of the data provider sending data to the CD11 dataman service
    :param dataman_ports: port range for the CD11 dataman service
    :return: string with --set arguments for helm
    """

    set_string = ''
    if injector:
        set_string += f'--set injector=True '
    if injector_dataset is not None:
        set_string += f'--set "cd11-injector.env.CD11_INJECTOR_CONFIG_NAME={injector_dataset}" '
    if livedata:
        set_string += f'--set "liveData=True" '
    if connman_port is not None:
        set_string += f'--set "da-connman.connPort={connman_port}" '
    if connman_data_manager_ip is not None:
        set_string += f'--set "da-connman.env.GMS_CONFIG_CONNMAN__DATA_MANAGER_IP_ADDRESS={connman_data_manager_ip}" '
    if connman_data_provider_ip is not None:
        set_string += f'--set "da-connman.env.GMS_CONFIG_CONNMAN__DATA_PROVIDER_IP_ADDRESS={connman_data_provider_ip}" '
    if dataman_ports is not None:
        set_string += f'--set "da-dataman.dataPortStart={dataman_ports.split("-")[0]}" '
        set_string += f'--set "da-dataman.dataPortEnd={dataman_ports.split("-")[1]}" '

    return set_string


def get_oracle_wallet_set_args(oracle_wallet_path):
    """
    Returns a string containing set arguments needed to include an Oracle Wallet
    :param oracle_wallet_path: path to the Oracle Wallet directory
    :return: string with --set arguments for helm
    """

    set_string = ''
    if oracle_wallet_path:
        if not os.path.isdir(oracle_wallet_path):
            print_error(f'The specified --oracle-wallet path ({oracle_wallet_path}) is not a directory...Exiting')
            exit(1)

        for filename in os.listdir(oracle_wallet_path):
            path = os.path.join(oracle_wallet_path, filename)
            if os.path.isfile(path):
                with open(path, 'rb') as fd:
                    contents = fd.read()
                    b64_contents = base64.b64encode(contents).decode('utf-8')
                    value_name = filename.replace(".", "\.")
                    set_string += f'--set "oracle-wallet.{value_name}={b64_contents}" '

    return set_string


def help_command(args, parser=None):
    parser.print_help()


def handler(signal_received, frame):
    # Handle any cleanup here
    exit(0)


if __name__ == "__main__":
    # register SIGINT handler for ctl-c
    signal(SIGINT, handler)

    main()
