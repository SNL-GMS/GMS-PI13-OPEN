export const profileReport = (
  id: string, // the "id" prop of the Profiler tree that has just committed
  phase: 'mount' | 'update', // either "mount" (if the tree just mounted) or "update" (if it re-rendered)
  actualDuration: number, // time spent rendering the committed update
  baseDuration: number, // estimated time to render the entire subtree without memoization
  startTime: number, // when React began rendering this update
  commitTime: number, // when React committed this update
  interactions: Set<any> // the Set of interactions belonging to this update
) => {
  // tslint:disable: no-console
  console.log(`${id} - phase: ${phase}`);
  console.log(`${id} - actualDuration: ${actualDuration}`);
  console.log(`${id} - baseDuration: ${baseDuration}`);
  console.log(`${id} - startTime: ${startTime}`);
  console.log(`${id} - commitTime: ${commitTime}`);
  console.log('renderTime', commitTime - startTime);
  console.dir(interactions);
};
