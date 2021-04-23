import { UILogger } from '../src/ts/ui-logger';
// tslint:disable: no-console
// tslint:disable: no-unbound-method

describe('UI Logger', () => {
  test('UI Logger should exist', () => {
    expect(UILogger).toBeDefined();
  });

  test('UI Logger should implement log level interface functions', () => {
    expect(UILogger.Instance().info).toBeDefined();
    expect(UILogger.Instance().data).toBeDefined();
    expect(UILogger.Instance().debug).toBeDefined();
    expect(UILogger.Instance().error).toBeDefined();
    expect(UILogger.Instance().log).toBeDefined();
    expect(UILogger.Instance().warn).toBeDefined();
  });

  test('UI Logger can be called', () => {
    const infoMock = jest.spyOn(UILogger.Instance(), 'info');
    UILogger.Instance().info('test1');
    expect(infoMock).toBeCalled();
    expect(infoMock).toBeCalledTimes(1);
    expect(infoMock).toBeCalledWith('test1');

    const warnMock = jest.spyOn(UILogger.Instance(), 'warn');
    UILogger.Instance().warn('test2');
    expect(warnMock).toBeCalled();
    expect(warnMock).toBeCalledTimes(1);
    expect(warnMock).toBeCalledWith('test2');

    const debugMock = jest.spyOn(UILogger.Instance(), 'debug');
    UILogger.Instance().debug('test3');
    expect(debugMock).toBeCalled();
    expect(debugMock).toBeCalledTimes(1);
    expect(debugMock).toBeCalledWith('test3');

    const logMock = jest.spyOn(UILogger.Instance(), 'log');
    UILogger.Instance().log('test4');
    expect(logMock).toBeCalled();
    expect(logMock).toBeCalledTimes(1);
    expect(logMock).toBeCalledWith('test4');

    const errorMock = jest.spyOn(UILogger.Instance(), 'error');
    UILogger.Instance().error('test5');
    expect(errorMock).toBeCalled();
    expect(errorMock).toBeCalledTimes(1);
    expect(errorMock).toBeCalledWith('test5');

    const dataMock = jest.spyOn(UILogger.Instance(), 'data');
    UILogger.Instance().data('test6');
    expect(dataMock).toBeCalled();
    expect(dataMock).toBeCalledTimes(1);
    expect(dataMock).toBeCalledWith('test6');
  });

  test('Log Popup Should Show when called', () => {
    // TODO: add mock single function
    expect(UILogger.Instance().showLogPopUpWindow).toBeDefined();
  });
});
