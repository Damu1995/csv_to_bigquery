package org.beam;

import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableReference;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;
import org.apache.beam.runners.dataflow.DataflowRunner;
import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.runners.direct.DirectRunner;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;

import java.util.ArrayList;
import java.util.List;

public class CSVToBigQuery {
  public interface MyOption extends PipelineOptions, DataflowPipelineOptions {

  }

  /**
   * Defines the BigQuery schema used for the output.
   */

  static TableSchema getSchema() {
    List<TableFieldSchema> fields = new ArrayList<>();
    // Currently store all values as String
    fields.add(new TableFieldSchema().setName("ID").setType("STRING"));
    fields.add(new TableFieldSchema().setName("Code").setType("STRING"));
    fields.add(new TableFieldSchema().setName("Value").setType("STRING"));
    fields.add(new TableFieldSchema().setName("Date").setType("STRING"));

    return new TableSchema().setFields(fields);
  }

  public static class ConvertToBigQueryRow extends DoFn<String, TableRow> {

    private static String HEADERS = "ID,Code,Value,Date";

    private String[] columnNames = HEADERS.split(",");

    @ProcessElement
    public void processElement(ProcessContext c) {
      TableRow row = new TableRow();
      String[] parts = c.element().split(",");

      if (!c.element().contains(HEADERS)) {
        for (int i = 0; i < parts.length; i++) {
          // No type conversion at the moment.
          row.set(columnNames[i], parts[i]);
        }
        c.output(row);
      }
    }
  }


  public static void main(String[] args) {
    MyOption options = PipelineOptionsFactory.as(MyOption.class);
    // need to modify these
    options.setProject("sales-products-phones-prj-02");
    options.setRunner(DataflowRunner.class);
    options.setJobName("CSV-TO-BigQuery");
    options.setRegion("us-central-1");
    options.setTempLocation("gs://ap1e234324/tmp");
    Pipeline pipeline = Pipeline.create(options);

    String sourceFilePath = "gs://ap1e234324/data.csv";
    TableReference tableRef = new TableReference();
    // Replace this with your own GCP project id
    tableRef.setProjectId("sales-products-phones-prj-02");
    tableRef.setDatasetId("ds1");
    tableRef.setTableId("test1");

    pipeline.apply("Read CSV File", TextIO.read().from(sourceFilePath))
            .apply("Convert data to BiqQuery row", ParDo.of(new ConvertToBigQueryRow()))
            .apply(" Write To BigQuery", BigQueryIO.writeTableRows().to(tableRef).withSchema(getSchema())
                    .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
                    .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_APPEND));
    pipeline.run();
  }
}
