import { dragEventIsOfType, DragEventType, getDragData } from '@gms/ui-util';
import { Cancelable, debounce } from 'lodash';
import * as React from 'react';

export interface DropZoneProps<Payload> {
  className?: string;
  onDrop(payload: Payload): void;
}

export interface DropZoneState {
  isHighlighted: boolean;
}

export class DropZone<Payload> extends React.Component<
  React.PropsWithChildren<DropZoneProps<Payload>>,
  DropZoneState
> {
  private readonly setStateTryMs: number = 200;

  private readonly debouncedSetStateHighlight: (() => void) & Cancelable = debounce(
    () => {
      this.setState({ isHighlighted: true });
    },
    this.setStateTryMs,
    { leading: true }
  );

  public constructor(props) {
    super(props);
    this.state = {
      isHighlighted: false
    };
  }

  public render() {
    return (
      <div
        className={`drop-zone
          ${this.state.isHighlighted ? 'drop-zone--highlighted' : ''}
          ${this.props.className ?? ''}`}
        onDragOver={this.cellDragOver}
        onDrop={e => this.cellDrop(e)}
      >
        {this.props.children}
      </div>
    );
  }

  /**
   * Cell drag over drop zone logic, checks if supported, data in transfer object and sets drop effect
   * @param event React.DragEvent<HTMLDivElement>
   */
  private readonly cellDragOver = (e: React.DragEvent<HTMLDivElement>): void => {
    const event = e.nativeEvent;
    if (dragEventIsOfType(e, DragEventType.SOH_ACKNOWLEDGEMENT)) {
      event.stopPropagation();
      event.preventDefault();
    }
  }

  /**
   * @param event React.DragEvent<HTMLDivElement>
   */

  /**
   * Cell drag logic, gets data from transfer object and calls acknowledgeSohStatus
   * @param event React.DragEvent<HTMLDivElement>
   * @param context context data for an soh panel
   */
  private readonly cellDrop = (event: React.DragEvent<HTMLDivElement>): void => {
    const payload = getDragData<Payload>(event, DragEventType.SOH_ACKNOWLEDGEMENT);
    this.debouncedSetStateHighlight.cancel();
    this.setState({ isHighlighted: false });
    this.props.onDrop(payload);
  }
}
