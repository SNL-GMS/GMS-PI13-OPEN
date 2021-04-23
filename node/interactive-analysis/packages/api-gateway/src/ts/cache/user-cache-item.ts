import Immutable from 'immutable';
import cloneDeep from 'lodash/cloneDeep';
import { mergeUserAndGlobalCache } from './cache-utils';
import { CommitFunc, IUserCacheItem } from './model';

/**
 * A User Cache Item used for managing local user cache changes
 * before being committed to the global cache.
 * Part of a user session to track changes to data until committed (saved).
 */
export class UserCacheItem<T extends { id: string }> implements IUserCacheItem<T> {
  /** The local cache data (actual changed data) */
  private data: Immutable.Map<string, T> = Immutable.Map<string, T>();

  /** The global cache */
  private globalCache: Immutable.Map<string, T>;

  /** The commit function used to commit back to the global cache */
  private readonly commit: CommitFunc<T>;

  /**
   * Constructor
   */
  public constructor(globalCache: Immutable.Map<string, T>, commit: CommitFunc<T>) {
    this.globalCache = globalCache;
    this.commit = commit;
  }

  /**
   * Returns true if the data exists for the given id.
   * @param id to check
   * @returns boolean
   */
  public has(id: string): boolean {
    return this.get(id) !== undefined;
  }

  /**
   * Get the data for the given id.
   * @param id to retrieve data for
   * @returns T
   */
  public get(id: string): T {
    return this.getOverlay().find(item => item.id === id);
  }

  /**
   * Get all data. Returns all global cache items
   * with the local cache items overlaid on the list.
   * @returns T[] all items
   */
  public getAll(): T[] {
    return this.getOverlay();
  }

  /**
   * Set the item on the local cache
   * @param item the item to set
   */
  public set(item: T): void {
    this.data = this.data.set(item.id, item);
  }

  /**
   * Set all of the items on the local cache
   * @param items the items to set
   */
  public setAll(items: T[]): void {
    items.forEach(item => this.set(item));
  }

  /**
   * Removes the item on the local cache, if it exists.
   * @param item the item to remove
   */
  public remove(item: T): void {
    this.data = this.data.remove(item.id);
  }

  /**
   * Removes the item on the local cache, if it exists.
   * @param items[] the items to remove
   */
  public removeAll(items: T[]): void {
    this.data = this.data.removeAll([...items.map(item => item.id)]);
  }

  /**
   * Commit all of the changes from the local cache.
   * Clears the local cache.
   */
  public commitAll(): void {
    this.commitWithIds([...this.data.keys()]);
  }

  /**
   * Commit the local changes for the given ids.
   * Clears the local changes for the given ids.
   * @param ids the ids to commit
   */
  public commitWithIds(ids: string[]): void {
    const values = [...this.data.values()].filter(item => ids.find(id => item.id === id));
    this.data = this.data.filter(item => !ids.find(id => item.id === id));
    this.commit(values);
  }

  /**
   * Updates the local cache with changes that have been made to the global cache.
   * @param items the global cache items that have changed
   * @param overwrite if true, the user cache will be overwritten with new data
   */
  public updateFromGlobalCache(items: T[], overwrite: boolean): void {
    // overwrite the user cache with the updated global cache entries
    items.forEach(item => {
      this.globalCache = this.globalCache.set(item.id, cloneDeep(item));
    });

    if (overwrite) {
      this.data = this.data.filter(item => !items.find(globalItem => globalItem.id === item.id));
    }
  }

  /**
   * Returns all of the data items where the global cache items are
   * overlaid with the local cache items.
   * @returns T
   */
  private getOverlay(): T[] {
    const items = [...this.data.values()];
    const globalItems = [...this.globalCache.values()];
    return mergeUserAndGlobalCache(items, globalItems) as T[];
  }
}
