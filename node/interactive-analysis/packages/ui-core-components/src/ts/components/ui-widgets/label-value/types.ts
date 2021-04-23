import React from 'react';

export interface LabelValueProps {
  value: string;
  label: string;
  tooltip: string;
  valueColor?: string;
  styleForValue?: React.CSSProperties;
}
