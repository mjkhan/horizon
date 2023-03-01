CREATE DATABASE HRZN_EXAMPLE;

USE HRZN_EXAMPLE;

CREATE TABLE CUSTOMER (
       CUST_ID              VARCHAR(5),
       CUST_NAME            VARCHAR(30) NOT NULL,
       ADDRESS              VARCHAR(50),
       PHONE_NO             VARCHAR(15) NOT NULL,
       EMAIL                VARCHAR(30) NOT NULL,
       CREDIT               INT,
       INS_TIME             DATETIME    NOT NULL,
       UPD_TIME             DATETIME    NOT NULL,
       PRIMARY KEY (CUST_ID)
);

INSERT INTO CUSTOMER (CUST_ID, CUST_NAME, ADDRESS, PHONE_NO, EMAIL, CREDIT, INS_TIME, UPD_TIME)
SELECT '00001', 'Jane East', 'Somewhere east', '01-001-0001', 'jane.east@acme.com', 10000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP UNION
SELECT '00002', 'Jane West', 'Somewhere west', '02-002-0002', 'jane.west@acme.com', 20000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP UNION
SELECT '00003', 'Jane South', 'Somewhere south', '03-003-0003', 'jane.south@acme.com', 30000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP UNION
SELECT '00004', 'Jane North', 'Somewhere north', '04-004-0004', 'jane.north@acme.com', 40000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP UNION
SELECT '00005', 'John East', 'Somewhere east', '05-005-0005', 'john.east@acme.com', 50000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP UNION
SELECT '00006', 'John West', 'Somewhere west', '06-006-0006', 'john.west@acme.com', 60000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP UNION
SELECT '00007', 'John South', 'Somewhere south', '07-007-0007', 'john.south@acme.com', 70000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP UNION
SELECT '00008', 'John North', 'Somewhere north', '08-008-0008', 'john.north@acme.com', 80000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP;

CREATE TABLE PRODUCT (
       PROD_ID              INT NOT NULL AUTO_INCREMENT,
       PROD_NAME            VARCHAR(30) NOT NULL,
       PROD_TYPE            VARCHAR(15) NOT NULL,
       UNIT_PRICE           INT         NOT NULL,
       VENDOR				VARCHAR(30),
       
       PRIMARY KEY (PROD_ID)
);

INSERT INTO PRODUCT (PROD_NAME, PROD_TYPE, UNIT_PRICE, VENDOR)
SELECT PROD_NAME, PROD_TYPE, UNIT_PRICE, VENDOR
FROM (
	SELECT 'JWord' PROD_NAME, 'Ohracle' VENDOR UNION
	SELECT 'JExcel' PROD_NAME, 'Macrosoft' VENDOR UNION
	SELECT 'JPoint' PROD_NAME, 'Joogle' VENDOR
) A, (
	SELECT 'Standard' PROD_TYPE, 1000 UNIT_PRICE UNION
	SELECT 'Professional' PROD_TYPE, 2000 UNIT_PRICE UNION
	SELECT 'Suite' PROD_TYPE, 3000 UNIT_PRICE
) B
ORDER BY PROD_NAME, UNIT_PRICE;

CREATE TABLE SALES_ORDER (
       ORD_ID               VARCHAR(5),
       ORD_DATE             DATE NOT NULL,
       CUST_ID              VARCHAR(5) NOT NULL,
       ORD_AMT              INTEGER NOT NULL DEFAULT 0,
       
       PRIMARY KEY (ORD_ID),
       FOREIGN KEY (CUST_ID) REFERENCES CUSTOMER(CUST_ID)
);

CREATE TABLE LINE_ITEM (
       ORD_ID               VARCHAR(5),
       LINE_ID              VARCHAR(3),
       PROD_ID              INTEGER NOT NULL,
       QNTY                 INTEGER NOT NULL,
       PRICE                INTEGER,
       
       PRIMARY KEY (ORD_ID, LINE_ID),
       FOREIGN KEY (ORD_ID) REFERENCES SALES_ORDER(ORD_ID),
       FOREIGN KEY (PROD_ID) REFERENCES PRODUCT(PROD_ID)
);

CREATE TABLE BUSI_ORG (
       ORG_ID              VARCHAR(5)  NOT NULL,
       ORG_TYPE            VARCHAR(3)  NOT NULL,
       ORG_NAME            VARCHAR(30) NOT NULL,
       PRNT_ORG            VARCHAR(5),
       PRIMARY KEY (ORG_ID)
);

INSERT INTO BUSI_ORG (ORG_ID, ORG_TYPE, ORG_NAME, PRNT_ORG)
SELECT '00000', '000', 'Company', NULL UNION
SELECT '00001', '001', 'Division A', '00000' UNION
SELECT '00002', '001', 'Division B', '00000' UNION
SELECT '00003', '002', 'Department A-0', '00001' UNION
SELECT '00004', '002', 'Department A-1', '00001' UNION
SELECT '00005', '002', 'Department A-2', '00001' UNION
SELECT '00006', '002', 'Department B-0', '00002' UNION
SELECT '00007', '002', 'Department B-1', '00002' UNION
SELECT '00008', '002', 'Department B-2', '00002';

DELIMITER $$

create procedure sp_all_customers() begin
    SELECT * FROM CUSTOMER;
end$$

create procedure sp_get_customers(custName varchar(30), amount int) begin
    SELECT * FROM CUSTOMER
    WHERE CUST_NAME LIKE CONCAT(custName, '%')
    AND CREDIT > amount;
end$$

create procedure sp_paginate_customers(amount int, start int, fetchSize int) begin
    SELECT SQL_CALC_FOUND_ROWS * FROM CUSTOMER
    WHERE CREDIT > amount
    LIMIT start, fetchSize;
    
    SELECT FOUND_ROWS() TOT_CNT;
end$$

create procedure sp_paginate_customers_outs(amount int, start int, fetchSize int, out totalCount int, out queryTime varchar(30)) begin
    SELECT SQL_CALC_FOUND_ROWS * FROM CUSTOMER
    WHERE CREDIT > amount
    LIMIT start, fetchSize;
    
    SELECT FOUND_ROWS() INTO totalCount;
    SELECT DATE_FORMAT(CURRENT_TIMESTAMP(), '%Y-%m-%d %H:%i:%s') INTO queryTime;
end$$

create procedure sp_get_outs(name varchar(30), out greeting varchar(30)) begin
    SELECT CONCAT('Hello ', name) INTO greeting;
end$$

create procedure sp_insert_customer(custID varchar(5), custName varchar(30)) begin
	INSERT INTO CUSTOMER (
		CUST_ID,
		CUST_NAME,
		ADDRESS,
		PHONE_NO,
		EMAIL,
		CREDIT,
		INS_TIME,
		UPD_TIME
	) VALUES (
		custID,
		custName,
		'Somewhere you may know',
		'99-999-9999',
		CONCAT(custID, '@acme.com'),
		10000,
		CURRENT_TIMESTAMP,
		CURRENT_TIMESTAMP
	);
end$$

create procedure sp_insert_customer_out(out custID varchar(5), custName varchar(30)) begin
	SELECT LPAD(IFNULL(MAX(CUST_ID) + 1, 1), 5, '0') INTO custID
	FROM CUSTOMER;
	
	INSERT INTO CUSTOMER (
		CUST_ID,
		CUST_NAME,
		ADDRESS,
		PHONE_NO,
		EMAIL,
		CREDIT,
		INS_TIME,
		UPD_TIME
	) VALUES (
		custID,
		custName,
		'Somewhere you may know',
		'99-999-9999',
		CONCAT(custID, '@acme.com'),
		10000,
		CURRENT_TIMESTAMP,
		CURRENT_TIMESTAMP
	);
end$$

DELIMITER ;
