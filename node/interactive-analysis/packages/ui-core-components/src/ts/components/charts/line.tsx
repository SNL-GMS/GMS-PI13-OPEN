import * as React from 'react';
import { VictoryLine } from 'victory';
import { Domain, LineChartProps } from './types';

/* border color */
const BORDER_COLOR = '#ffffff';

/**
 * Line chart - renders a Victory line chart component with axis
 */
export const Lines: React.FunctionComponent<LineChartProps> = props => {
  const [domain] = props.domain ? [props.domain] : React.useState<Domain>();
  return (
    <React.Fragment>
      {props.lineDefs
        ? props.lineDefs.map((lineDef, index) => (
            <VictoryLine
              {...props}
              domain={domain}
              key={index}
              style={{
                data: {
                  stroke: lineDef.color,
                  strokeWidth: 1
                },
                parent: { border: `1px solid ${BORDER_COLOR}` }
              }}
              interpolation={props.interpolation ? props.interpolation : 'linear'}
              data={lineDef.value}
            />
          ))
        : undefined}
    </React.Fragment>
  );
};
