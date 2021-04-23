import cond from 'lodash/cond';
import identity from 'lodash/identity';
import stubTrue from 'lodash/stubTrue';
import transit from 'transit-js';

export const transitToObj: (item: any) => any = cond([
  [
    transit.isMap,
    item =>
      Object.assign(
        {},
        ...Array.from<any>(item.entries()).map(([k, v]) => ({
          [k.name()]: transitToObj(v)
        }))
      )
  ],
  // tslint:disable-next-line:no-unbound-method
  [Array.isArray, (item: [any]) => item.map(transitToObj)],
  [transit.isKeyword, item => item.name()],
  [stubTrue, identity]
]);

export const transitReader = transit.reader('json', {
  handlers: { list: identity }
});
