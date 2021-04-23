import { Table } from '@gms/ui-core-components/lib/components/table';
import { AgGridReactProps } from 'ag-grid-react';
import { mount } from 'enzyme';
import React from 'react';
import { getRowData } from '../../../../__data__/events-table-data';

describe('ui core components table', () => {
  test('basic functions', (done: jest.DoneCallback) => {
    const wrapper = mount(<Table id="events-table" key="events-table" />);

    const r = getRowData(false);

    wrapper.setProps({ rowData: r });

    setImmediate(() => {
      wrapper.update();

      expect(wrapper).toBeDefined();

      expect(wrapper.exists()).toEqual(true);

      expect(wrapper.props().rowData).toBeDefined();

      let agGridProps: AgGridReactProps = wrapper.find('AgGridReact').props() as AgGridReactProps;

      // row data is null because that is what is set on the props for underlying table
      expect(agGridProps.rowData).toBeNull();

      // TODO check the underlying data from the table component

      const newR = getRowData(true);
      wrapper.setProps({ rowData: newR });
      agGridProps = wrapper
        .update()
        .find('AgGridReact')
        .props() as AgGridReactProps;
      expect(agGridProps.rowData).toBeNull();

      done();
    });
  });
});
