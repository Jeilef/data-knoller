package de.hpi.isg.dataprep.preparators;

import de.hpi.isg.dataprep.implementation.defaults.DefaultAddPropertyImpl;
import de.hpi.isg.dataprep.implementation.defaults.DefaultChangePropertyDataTypeImpl;
import de.hpi.isg.dataprep.model.repository.ErrorRepository;
import de.hpi.isg.dataprep.model.target.Pipeline;
import de.hpi.isg.dataprep.model.target.Preparation;
import de.hpi.isg.dataprep.model.target.errorlog.ErrorLog;
import de.hpi.isg.dataprep.model.target.errorlog.PreparationErrorLog;
import de.hpi.isg.dataprep.model.target.preparator.Preparator;
import de.hpi.isg.dataprep.util.DataType;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Lan Jiang
 * @since 2018/6/4
 */
public class PipelineTest {

    private static Dataset<Row> dataset;
    private static Pipeline pipeline;

    @BeforeClass
    public static void setUp() {
        Logger.getLogger("org").setLevel(Level.OFF);
        Logger.getLogger("akka").setLevel(Level.OFF);

        dataset = SparkSession.builder()
                .appName("Rename property unit tests.")
                .master("local")
                .getOrCreate()
                .read()
                .option("header", "true")
                .option("inferSchema", "true")
                .csv("./src/test/resources/pokemon.csv");
        pipeline = new Pipeline(dataset);
    }

    @Before
    public void cleanUpPipeline() throws Exception {
        pipeline = new Pipeline(dataset);
    }

    @Test
    public void testShortPipeline() throws Exception {
        Preparator preparator1 = new ChangePropertyDataType(new DefaultChangePropertyDataTypeImpl());
        ((ChangePropertyDataType) preparator1).setPropertyName("id");
        ((ChangePropertyDataType) preparator1).setTargetType(DataType.PropertyType.INTEGER);

        Preparation preparation1 = new Preparation(preparator1);
        pipeline.addPreparation(preparation1);

        Preparator preparator2 = new AddProperty(new DefaultAddPropertyImpl());
        ((AddProperty) preparator2).setTargetPropertyName("ship");
        ((AddProperty) preparator2).setTargetPropertyDataType(DataType.PropertyType.DATE);
        ((AddProperty) preparator2).setPositionInSchema(5);

        Preparation preparation2 = new Preparation(preparator2);
        pipeline.addPreparation(preparation2);

        pipeline.executePipeline();

        List<ErrorLog> trueErrorlogs = new ArrayList<>();
        ErrorLog errorLog1 = new PreparationErrorLog(preparation1, "three", new NumberFormatException("For input string: \"three\""));
        ErrorLog errorLog2 = new PreparationErrorLog(preparation1, "six", new NumberFormatException("For input string: \"six\""));
        ErrorLog errorLog3 = new PreparationErrorLog(preparation1, "ten", new NumberFormatException("For input string: \"ten\""));

        trueErrorlogs.add(errorLog1);
        trueErrorlogs.add(errorLog2);
        trueErrorlogs.add(errorLog3);
        ErrorRepository trueErrorRepository = new ErrorRepository(trueErrorlogs);

        Assert.assertEquals(trueErrorRepository, pipeline.getErrorRepository());

        Dataset<Row> updated = pipeline.getRawData();
        StructType updatedSchema = updated.schema();

        StructType trueSchema = new StructType(new StructField[] {
                new StructField("id", DataTypes.StringType, true, Metadata.empty()),
                new StructField("identifier", DataTypes.StringType, true, Metadata.empty()),
                new StructField("species_id", DataTypes.IntegerType, true, Metadata.empty()),
                new StructField("height", DataTypes.IntegerType, true, Metadata.empty()),
                new StructField("weight", DataTypes.IntegerType, true, Metadata.empty()),
                new StructField("ship", DataTypes.DateType, true, Metadata.empty()),
                new StructField("base_experience", DataTypes.IntegerType, true, Metadata.empty()),
                new StructField("order", DataTypes.IntegerType, true, Metadata.empty()),
                new StructField("is_default", DataTypes.IntegerType, true, Metadata.empty()),
                new StructField("date", DataTypes.StringType, true, Metadata.empty()),
        });

        // Second test whether the schema is correctly updated.
        Assert.assertEquals(trueSchema, updatedSchema);
        Assert.assertEquals(updated.count(), 7L);
        Assert.assertEquals(updatedSchema.size(), 10);
    }
}
