import React from 'react';
import { TextAreaState } from '../widgets/types';
import { TextAreaProps } from './types';

export class TextArea extends React.Component<TextAreaProps, TextAreaState> {
  private constructor(props) {
    super(props);
    this.state = {
      // optional can be undefined
      charsLeft: this.props.maxChar,
      // optional can be undefined
      maxChar: this.props.maxChar,
      isValid: true,
      value: this.props.defaultValue
    };
  }

  /**
   * React component lifecycle.
   */
  public render() {
    return (
      <div>
        <textarea
          className="form__text-input"
          rows={4}
          title={this.props.title}
          data-cy={this.props['data-cy'] ? `${this.props['data-cy']}-textarea` : 'textarea'}
          onChange={e => {
            // handle case where we dont want to do character count
            const charsLeft = this.state.maxChar
              ? this.state.maxChar - e.currentTarget.value.length
              : undefined;
            this.setState({
              ...this.state,
              value: e.currentTarget.value,
              charsLeft
            });
            this.props.onMaybeValue(e.currentTarget.value);
          }}
          maxLength={this.state.maxChar}
          value={this.state.value}
        />
        {this.state.maxChar && (
          <p className="form__character-count">Characters remaining: {this.state.charsLeft}</p>
        )}
      </div>
    );
  }
}
