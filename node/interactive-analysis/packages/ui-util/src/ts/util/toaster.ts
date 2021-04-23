import {
  IconName,
  Intent,
  IToaster,
  Position,
  Toaster as BlueprintToaster,
  ToasterPosition
} from '@blueprintjs/core';
import { IconNames } from '@blueprintjs/icons';

/**
 * Wrapper around blueprint js toaster. Used for warning, errors, and messages in the UI
 */
export class Toaster {
  /** Default timeout in milliseconds */
  private readonly defaultTimeout: number = 4000;

  /** The toaster reference for user notification pop-ups */
  private readonly toaster: IToaster;

  /**
   * constructor
   *
   * @param position blueprint js position, default is Position.BOTTOM_RIGHT
   */
  public constructor(position: ToasterPosition = Position.BOTTOM_RIGHT) {
    this.toaster = BlueprintToaster.create({ position });
  }

  /**
   * Display a toast message.
   *
   * @param message string to be toasted
   * @param intent blueprint js intent color scheme
   * @param icon blueprint js icon to be shown in toast
   * @param timeout milliseconds message is displayed
   */
  public readonly toast = (message: string, intent?: Intent, icon?: IconName, timeout?: number) => {
    if (this.toaster) {
      // Only display unique messages
      if (this.toaster.getToasts().length > 0) {
        if (this.toaster.getToasts().find(toast => toast.message === message)) {
          return;
        }
      }
      this.toaster.show({
        message,
        intent: intent ? intent : Intent.NONE,
        icon: icon ? icon : IconNames.INFO_SIGN,
        timeout: !timeout ? this.defaultTimeout : timeout
      });
    }
  }

  /**
   * Display a INFO toast message.
   *
   * @param message string to be toasted
   * @param timeout milliseconds message is displayed
   */
  public readonly toastInfo = (message: string, timeout?: number) => {
    this.toast(message, Intent.NONE, IconNames.INFO_SIGN, timeout);
  }

  /**
   * Display a WARNING toast message.
   *
   * @param message string to be toasted
   * @param timeout milliseconds message is displayed
   */
  public readonly toastWarn = (message: string, timeout?: number) => {
    this.toast(message, Intent.WARNING, IconNames.WARNING_SIGN, timeout);
  }

  /**
   * Display a ERROR toast message.
   *
   * @param message string to be toasted
   * @param timeout milliseconds message is displayed
   */
  public readonly toastError = (message: string, timeout?: number) => {
    this.toast(message, Intent.DANGER, IconNames.ERROR, timeout);
  }
}
