export interface DropDownProps {
  value: string;
  dropDownItems: any;
  dropdownText?: any;
  widthPx?: number;
  disabled?: boolean;
  title?: string;
  custom?: boolean;
  'data-cy'?: string;
  className?: string;
  onMaybeValue(value: any);
}
