import { UILogger } from '@gms/ui-apollo';
import React, { createRef, useEffect, useState } from 'react';

interface SoundSampleProps {
  soundToPlay: string;
}

export const SoundSample: React.FunctionComponent<SoundSampleProps> = (props: SoundSampleProps) => {
  const ref = createRef<HTMLAudioElement>();
  const filename: string = props.soundToPlay?.split('/').slice(-1)[0];

  const [play, setPlay] = useState(false);

  useEffect(() => {
    if (play && props.soundToPlay && ref.current && filename !== 'None') {
      ref.current.play().catch(e => {
        UILogger.Instance().error(`Error playing sound "${props.soundToPlay}": ${e}`);
      });
    }
    setPlay(true);
  }, [props.soundToPlay]);

  if (filename === 'None') {
    return null;
  }
  return <audio ref={ref} src={props.soundToPlay} autoPlay={false} />;
};
