DROP TABLE businesssrc;

CREATE TABLE businesssrc(cust_num INT, region_id INT, saler_id INT, year INT, month INT, tot_orders INT, tot_sales INT) ROW FORMAT DELIMITED FIELDS TERMINATED BY ',';

FROM businesssrc
SELECT cust_num, region_id, saler_id, year, month, LAG(tot_sales) OVER(partition by region_id order by region_id);

DROP TABLE businesssrc;