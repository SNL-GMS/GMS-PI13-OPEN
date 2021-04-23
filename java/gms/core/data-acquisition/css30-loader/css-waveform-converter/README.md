Command-line application that takes CSS (Center for Seismic Studies) data and some extra information and produces a file `state-of-health.json` 
containing a `AcquiredChannelSohBoolean[]` and either:
 - a directory of files named `segments-n.json` (`segments-1.json`, `segments-2.json`, ...) each containing a `ChannelSegment<Waveform>[]`
 - a single file `segment-claim-checks.json` containing a `SegmentClaimCheck[]`
 
The directory of 'segment claim check' files is produced when argument `waveformsDir` is null, otherwise the segment files with fully populated waveform
samples are produced.  The 'segment claim check' usage (`-waveformsDir` not provided) is common and the default because 
it is faster and avoids duplicating large waveform data on disk.

The program requires arguments that each specify an input file, plus one argument `-outputDir` that specifies where to write the output files. 
**All arguments except `-waveformsDir` are required.**

**These arguments specify CSS files:**
 - `-wfDiscFile`: a CSS wfdisc file

**These arguments specify special files that are not part of CSS:**
  - `-stationGroupFile`: a JSON file containing a `StationGroup[]`.  This is used in conjunction with `-wfidToChannelFile` to find a `Channel` for each `ChannelSegment` being produced.
   Typically, this file contains 'raw' channels only and is produced by running [css-stationref-converter](../css-stationref-converter).
 - `-wfidToChannelFile`: a JSON file containing a `Map<Long, Channel>`.  This is used in conjunction with `stationGroupFile` to find a `Channel` for each `ChannelSegment` being produced.
   Typically, this file contains 'derived' channels only and is produced by GMS Subject Matter Experts.
 - `-waveformsDir`: the directory where binary waveform files ('.w' files) referenced in the `wfDiscFile` can be found.  When provided, this argument is used to find these waveform
   files and populate waveforms with them.  When not provided, the name of the waveform file specified in a record of the `wfDiscFile` (the `dfile` element) is provided to the `SegmentClaimCheck`. 