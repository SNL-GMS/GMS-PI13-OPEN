Command-line application that takes CSS (Center for Seismic Studies) data and some extra information and produces JSON files containing GMS Event and SignalDetection objects. 
The program takes arguments that each specify an input file, plus one argument `-outputDir` that specifies where to write the output 
files `events.json` (containing an `Event[]`) and `signal-detections.json` (containing a `SignalDetection[]`). **All arguments are required.**

**These arguments specify CSS files:**
 - `-amplitude`: a CSS amplitude file
 - `-arrival`: a CSS arrival file
 - `-assoc`: a CSS assoc file
 - `-event`: a CSS event file
 - `-netmag`: a CSS netmag file
 - `-origerr`: a CSS origerr file
 - `-origin`: a CSS origin file
 - `-stamag`: a CSS stamag file
 - `-wfdisc`: a CSS wfdisc file
 
Some of these CSS files reference each other (for instance assoc references arrival) and so generally should form a cohesive set dataset.  
It might seem curious that a CSS wfdisc file is required to produce events and signal detections; this is because the feature measurements of signal detections
refer to the time range the measurement was made over; this time range is acquired from the corresponding wfdisc row (found via AridToWfid, see below).

**These arguments specify special files that are not part of CSS:**
 - `-aridToWfidFile`: a JSON file containing a map (JSON object) between CSS arid's (arrival id's, integer) and CSS wfid's (wfdisc id, integer).
 - `-wfidToChannelFile`: a JSON file containing a map (JSON object) between CSS wfid's (wfdisc id, integer) and GMS Channel object's.  This is used in combination with AridToWfid through the chain WfidToChannel(AridToWfid(arid)), producing a Channel for a signal detection used in creating feature measurements and feature predictions.