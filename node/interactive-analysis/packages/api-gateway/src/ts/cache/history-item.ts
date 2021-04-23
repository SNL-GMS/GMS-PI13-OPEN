import produce, { applyPatches, Patch, produceWithPatches } from 'immer';
import Immutable from 'immutable';
import isEqual from 'lodash/isEqual';
import merge from 'lodash/merge';
import { gatewayLogger as logger } from '../log/gateway-logger';

/**
 * Represents the patches for a change.
 *
 * Patches are the changes that allow you to go from the
 * original object state to the new object state.
 *
 * Inverse patches are the changes that allow you to go
 * from the new object state to the original object state.
 */
interface Patches {
  readonly patches: Patch[];
  readonly inversePatches: Patch[];
}

/**
 * Represents a change; contains the
 * updated value with the patches.
 */
interface HistoryItemChange<T> {
  readonly value: T;
  readonly changes: Patches;
}

/**
 * Manages the history of an item.
 */
export class HistoryItem<T> {
  /** the original starting state */
  private readonly original: { value: T };

  /** the current index set for the history stack */
  private idx: number;

  /** the history of changes */
  private history: Immutable.List<Patch[]>;

  /** Constructor */
  public constructor(original: T) {
    this.original = { value: original };

    this.idx = 0;
    this.history = Immutable.List<Patch[]>();
    this.history = this.history.push([]);
  }

  /** returns the current index into the history */
  public index(): number {
    return this.idx;
  }

  /** returns the size of the history stack */
  public size(): number {
    return this.history.size;
  }

  /**
   * Adds a new entry into the history.
   * @param historyChange the new history change to track
   */
  public add(value: T) {
    // check the current history, and reset if the index is not at the latest version
    // clear out the history (if in the future)
    this.history = this.history.slice(0, this.idx + 1);

    const changes = this.determineChanges(this.value(), value);

    // push to the history stack
    this.history = this.history.push(changes.changes.patches);

    // update the history index
    this.setIndex(this.idx + 1);
  }

  /**
   * Returns the current value.
   */
  public value(): T {
    return this.history.toArray().reduce<{ value: T }>((accumulator, value, index) => {
      if (index <= this.idx) {
        return produce<{ value: T }>(accumulator, draftState => applyPatches(draftState, value));
      }
      return accumulator;
    }, this.original).value;
  }

  /**
   * Undo a single change.
   */
  public undo(): T {
    this.setIndex(this.idx - 1);
    return this.value();
  }

  /**
   * Redo a single change.
   */
  public redo(): T {
    this.setIndex(this.idx + 1);
    return this.value();
  }

  /** clears the history */
  public clear(): void {
    this.idx = -1;
    this.history = this.history.clear();
  }

  /**
   * Validates the given index and ensures it is in a valid range.
   * @param index the index
   */
  private validateIndex(index: number): number {
    if (index < 0) {
      return 0;
    }

    if (index > this.history.size - 1) {
      return this.history.size - 1;
    }

    return index;
  }

  /**
   * Sets the current history index
   * @param index the index to set
   */
  private setIndex(index: number): void {
    this.idx = this.validateIndex(index);
  }

  /**
   * Prepares a object of type T for saving to the user cache.
   * Determines the changes that have been applied to the original signal detection.
   *
   * @param originalState the original object state
   * @param updatedState the updated object state
   */
  private determineChanges(originalState: T, updatedState: T): HistoryItemChange<T> {
    // TODO using merge does not guarantee that arrays, lists, maps, etc are handled correctly
    // !Figure out a better mechanism to determine the patches, for the time being
    // !validate the patches an correct if needed
    let [value, patches, inversePatches] = produceWithPatches<{ value: T }>(
      { value: originalState },
      draftState => {
        merge(draftState, { value: updatedState });
      }
    );

    // validate the object to return
    if (!isEqual(updatedState, value.value)) {
      logger.warn(`Failed to prepare individual object properties for save: objects do not equal`);
      // failure with the patches-> replace the entire object
      [value, patches, inversePatches] = produceWithPatches<{ value: T }>(
        { value: originalState },
        draftState => {
          draftState.value = { ...(updatedState as any) };
        }
      );
    }

    // validate the patches
    const validatePatches = applyPatches<{ value: T }>({ value: originalState }, patches);
    if (!isEqual(updatedState, validatePatches.value)) {
      logger.warn(`Failed to validate object patches for save: objects do not equal`);
      [value, patches, inversePatches] = produceWithPatches<{ value: T }>(
        { value: originalState },
        draftState => {
          draftState.value = { ...(updatedState as any) };
        }
      );
    }

    // validate the inverse patches
    const validateInversePatches = applyPatches<{ value: T }>(
      { value: updatedState },
      inversePatches
    );
    if (!isEqual(originalState, validateInversePatches.value)) {
      logger.warn(`Failed to validate object inverse patches for save: objects do not equal`);
      [value, patches, inversePatches] = produceWithPatches<{ value: T }>(
        { value: originalState },
        draftState => {
          draftState.value = { ...(updatedState as any) };
        }
      );
    }
    return { value: value.value, changes: { patches, inversePatches } };
  }
}
