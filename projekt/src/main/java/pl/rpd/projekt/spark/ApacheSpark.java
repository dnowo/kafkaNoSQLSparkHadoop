package pl.rpd.projekt.spark;

import lombok.val;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.*;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static pl.rpd.projekt.Constants.*;

@Component
public class ApacheSpark {

    private SparkSession sparkSession;

    @PostConstruct
    public void setup() {
        SparkConf sparkConf = new SparkConf();
        sparkConf.setMaster("local");
        sparkConf.setAppName("");
        sparkConf.set("spark.cassandra.connection.host", CASSANDRA_HOST);
        sparkConf.set("spark.cassandra.auth.username", CASSANDRA_USERNAME);
        sparkConf.set("spark.cassandra.auth.password", CASSANDRA_PASSWORD);
        sparkConf.set("spark.cassandra.output.consistency.level", "ONE");

        sparkSession = SparkSession.builder()
                .config(sparkConf)
                .getOrCreate();
    }

    public void proceedData() {
        Dataset<Row> crypto = sparkSession.read()
                .format("org.apache.spark.sql.cassandra")
                .option("keyspace", "crypto")
                .option("table", "cryptocurrencies")
                .load();
        Dataset<Row> dates = crypto.select("date", "open", "close").where("symbol = 'DOGE'");
        Encoder<MiniCryptoDto> timestampEncoder = Encoders.bean(MiniCryptoDto.class);
        Dataset<MiniCryptoDto> timestampDataset = dates.map((MapFunction<Row, MiniCryptoDto>) row -> {
            var r = row.getTimestamp(0).toInstant();
            if (r.isAfter(Instant.now().minus(365, ChronoUnit.DAYS))) {
                val mcdto = new MiniCryptoDto();
                mcdto.setOpen(row.getDecimal(1));
                mcdto.setClose(row.getDecimal(2));
                mcdto.setDate(Timestamp.from(r));
                return mcdto;
            }
            return null;
        }, timestampEncoder).filter(obj -> obj.getDate() != null);
        timestampDataset.show(30, false);
        JavaRDD<MiniCryptoDto> timestamps = timestampDataset.javaRDD();
        //zapisać jako para i posortować po datach, ewentualnie reduceByKey i po miesiącach zredukować


//        lines.flatMap(line -> {
//            String[] data = COMMA.split(line);
//            List<String> results = new ArrayList<>();
//
//            String id = data[1];
//            String name = data[2];
//            results.add(id + " " + name);
//            return results;
//        });
//
//        JavaRDD<Integer> amount = lines.flatMap(line -> {
//            String[] data = COMMA.split(line);
//            List<Integer> results = new ArrayList<>();
//
//            int returnAmount = Integer.parseInt(data[2]);
//            results.add(returnAmount);
//            return results;
//        });
//        JavaPairRDD<String, Integer> idNameWithAmount = idWithNameConcatenated.zip(amount);
//        idWithNameConcatenated.collect();
//        JavaPairRDD<String, Integer> counts = idNameWithAmount.reduceByKey(Integer::sum);
//        JavaPairRDD<String, Integer> sorted = counts.sortByKey();
//
//        sorted.coalesce(1).saveAsHadoopFile(outputPath, Text.class, IntWritable.class, SequenceFileOutputFormat.class);
//        sc.stop();
    }
}
