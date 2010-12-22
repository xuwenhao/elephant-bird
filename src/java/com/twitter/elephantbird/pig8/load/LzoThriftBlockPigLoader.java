package com.twitter.elephantbird.pig8.load;

import java.io.IOException;

import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.pig.Expression;
import org.apache.pig.LoadMetadata;
import org.apache.pig.ResourceSchema;
import org.apache.pig.ResourceStatistics;
import org.apache.pig.data.Tuple;
import org.apache.pig.impl.util.Pair;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.twitter.elephantbird.mapreduce.input.LzoThriftB64LineInputFormat;
import com.twitter.elephantbird.pig.piggybank.ThriftToPig;
import com.twitter.elephantbird.pig8.load.LzoBaseLoadFunc;
import com.twitter.elephantbird.util.ThriftUtils;
import com.twitter.elephantbird.util.TypeRef;


public class LzoThriftBlockPigLoader<M extends TBase<?>> extends LzoBaseLoadFunc implements LoadMetadata {
  private static final Logger LOG = LoggerFactory.getLogger(LzoThriftBlockPigLoader.class);

  private final TypeRef<M> typeRef_;
  private final ThriftToPig<M> thriftToPig_;

  private final Pair<String, String> thriftStructsRead;
  private final Pair<String, String> thriftErrors;

  public LzoThriftBlockPigLoader(String thriftClassName) {
    typeRef_ = ThriftUtils.getTypeRef(thriftClassName);
    thriftToPig_ =  ThriftToPig.newInstance(typeRef_);

    String group = "LzoBlocks of " + typeRef_.getRawClass().getName();
    thriftStructsRead = new Pair<String, String>(group, "Thrift Structs Read");
    thriftErrors = new Pair<String, String>(group, "Errors");

    setLoaderSpec(getClass(), new String[]{thriftClassName});
  }

  /**
   * Return every non-null line as a single-element tuple to Pig.
   */
  @Override
  public Tuple getNext() throws IOException {
    if (reader_ == null) {
      return null;
    }

    M value;
    try {
      while (reader_.nextKeyValue()) {
        try {
          value = (M) reader_.getCurrentValue();
          Tuple t = thriftToPig_.getPigTuple(value);
          incrCounter(thriftStructsRead, 1L);
          return t;
        } catch (TException e) {
          incrCounter(thriftErrors, 1L);
          LOG.warn("ThriftToTuple error :", e); // may be corrupt data.
          // try next
        }
      }
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public ResourceSchema getSchema(String filename, Job job) throws IOException {
    return new ResourceSchema(ThriftToPig.toSchema(typeRef_.getRawClass()));
  }


  /**
   * TODO
   * DOES NOT WORK - needs to get a jobconf
   */
  @Override
  public InputFormat getInputFormat() throws IOException {
    try {
      return LzoThriftB64LineInputFormat.getInputFormatClass(typeRef_.getRawClass(), null).newInstance();
    } catch (InstantiationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
  }

  /**
   * NOT IMPLEMENTED
   */
  @Override
  public String[] getPartitionKeys(String arg0, Job arg1) throws IOException {
    return null;
  }

  /**
   * NOT IMPLEMENTED
   */
  @Override
  public ResourceStatistics getStatistics(String arg0, Job arg1) throws IOException {
    return null;
  }

  /**
   * NOT IMPLEMENTED
   */
  @Override
  public void setPartitionFilter(Expression arg0) throws IOException {

  }
}
