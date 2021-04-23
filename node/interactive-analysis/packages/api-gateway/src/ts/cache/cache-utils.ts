/**
 * Merges user and global caches.
 * @param userData the user cache data
 * @param globalData the global cache data
 * @returns the merged object
 */
export function mergeUserAndGlobalCache(
  userData: { id: string }[],
  globalData: { id: string }[]
): any {
  // First go thru the global data overlying any modified user data
  const toReturn = globalData.map(gItem => {
    const userItem = userData.find(usd => usd.id === gItem.id);
    if (userItem) {
      return userItem;
    }
    return gItem;
  });

  // Then add all of the new user data
  userData.forEach(uItem => {
    const globalItem = globalData.find(gItem => gItem.id === uItem.id);
    if (!globalItem) {
      toReturn.push(uItem);
    }
  });
  if (!toReturn) {
    return [];
  }
  return toReturn;
}
