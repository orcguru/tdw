/**
* Tencent is pleased to support the open source community by making TDW available.
* Copyright (C) 2014 THL A29 Limited, a Tencent company. All rights reserved.
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use 
* this file except in compliance with the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software distributed 
* under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS 
* OF ANY KIND, either express or implied. See the License for the specific language governing
* permissions and limitations under the License.
*/
package StorageEngineClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.hadoop.fs.BlockLocation;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;

import FormatStorage1.IFormatDataFile;
import FormatStorage1.IRecord;
import FormatStorage1.ISegmentIndex;

@SuppressWarnings("deprecation")
public class FormatStorageInputFormat<K, V> extends
    FileInputFormat<LongWritable, IRecord> {
  public static final Log LOG = LogFactory
      .getLog(FormatStorageInputFormat.class);

  public FormatStorageInputFormat() {
  }

  public RecordReader<LongWritable, IRecord> getRecordReader(InputSplit split,
      JobConf job, Reporter reporter) throws IOException {
    return new FormatStorageIRecordReader(job, (FormatStorageInputSplit) split);
  }

  public InputSplit[] getSplits(JobConf job, int numSplits) throws IOException {
    Path tmpPath = null;
    List<FormatStorageInputSplit> splits = new ArrayList<FormatStorageInputSplit>();
    for (FileStatus file : listStatus(job)) {
      Path path = file.getPath();
      tmpPath = path;
      FileSystem fs = path.getFileSystem(job);
      long length = file.getLen();

      BlockLocation[] blkLocations = fs.getFileBlockLocations(file, 0, length);
      if (blkLocations.length == 0) {
        continue;
      } else if (blkLocations.length == 1) {
        FormatStorageInputSplit split = new FormatStorageInputSplit(path,
            length, blkLocations[0].getHosts());
        splits.add(split);
      } else {
        String filename = path.toString();
        IFormatDataFile ifdf = new IFormatDataFile(job);
        ifdf.open(filename);

        ISegmentIndex segmentIndex = ifdf.segIndex();
        for (int i = 0; i < segmentIndex.getSegnum(); i++) {
          blkLocations = fs.getFileBlockLocations(file,
              segmentIndex.getSegOffset(i), segmentIndex.getseglen(i));

          FormatStorageInputSplit split = new FormatStorageInputSplit(path,
              segmentIndex.getseglen(i), segmentIndex.getILineIndex(i)
                  .beginline(), segmentIndex.getILineIndex(i).endline()
                  - segmentIndex.getILineIndex(i).beginline() + 1,
              blkLocations[0].getHosts());
          splits.add(split);
        }
        ifdf.close();
      }
    }

    if (splits.size() == 0) {
      splits.add(new FormatStorageInputSplit(tmpPath, 0, 0, 0, new String[0]));
    }

    LOG.info("Total # of splits: " + splits.size());
    return splits.toArray(new FormatStorageInputSplit[splits.size()]);
  }
}
