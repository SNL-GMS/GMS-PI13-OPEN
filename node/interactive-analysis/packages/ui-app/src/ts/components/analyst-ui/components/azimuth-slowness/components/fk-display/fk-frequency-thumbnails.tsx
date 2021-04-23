import { FkTypes } from '@gms/common-graphql';
import React from 'react';
import { frequencyBandToString } from '~analyst-ui/common/utils/fk-utils';
import { FkUnits } from '../../types';
import { FkThumbnail } from '../fk-thumbnail';

const SIZE_PX_OF_FREQUENCY_THUMBNAILS_PX = 100;

export interface FkFrequencyThumbnailProps {
  fkFrequencySpectra: FkTypes.FkFrequencyThumbnail[];
  fkUnit: FkUnits;
  arrivalTimeMovieSpectrumIndex: number;

  onThumbnailClick(minFrequency: number, maxFrequency: number): void;
}
export class FkFrequencyThumbnails extends React.Component<FkFrequencyThumbnailProps, {}> {
  public render() {
    return (
      <div className="fk-frequency-thumbnails">
        {this.props.fkFrequencySpectra.map((spectra, index) => (
          <FkThumbnail
            fkData={spectra.fkSpectra}
            label={frequencyBandToString(spectra.frequencyBand)}
            key={index}
            selected={false}
            dimFk={false}
            sizePx={SIZE_PX_OF_FREQUENCY_THUMBNAILS_PX}
            fkUnit={this.props.fkUnit}
            showFkThumbnailMenu={() => {
              return;
            }}
            arrivalTimeMovieSpectrumIndex={0}
            onClick={() => {
              this.props.onThumbnailClick(
                spectra.frequencyBand.minFrequencyHz,
                spectra.frequencyBand.maxFrequencyHz
              );
            }}
          />
        ))}
      </div>
    );
  }
}
