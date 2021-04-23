import React from 'react';
import { TextFormats } from '../../types';

/**
 * Props for form text display
 */
export interface FormDisplayTextProps {
  displayText: string;
  tooltip?: string;
  formatAs?: TextFormats;
  widthPx: number;
}
/**
 * Displays text for Form
 */
export class FormDisplayText extends React.Component<FormDisplayTextProps, {}> {
  private constructor(props) {
    super(props);
  }
  /**
   * Renders the component
   */
  public render() {
    const className =
      this.props.formatAs === TextFormats.Time
        ? 'form-value form-value--uneditable form-value--time'
        : 'form-value form-value--uneditable';
    return (
      <div
        className={className}
        style={{ width: `${this.props.widthPx}px` }}
        title={this.props.tooltip}
      >
        {this.props.displayText}
      </div>
    );
  }
}
