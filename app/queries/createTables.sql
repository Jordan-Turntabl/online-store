--Add UUID_GENERATE extension to local postgres
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

--Create enum type for order status
CREATE TYPE order_status AS ENUM ( 'Queued', 'Processing', 'Failed','Success');

--Create all database tables
CREATE TABLE IF NOT EXISTS customers
(
    customer_id UUID NOT NULL,
    full_name character varying(40) COLLATE pg_catalog."default" NOT NULL,
    email character varying(40) COLLATE pg_catalog."default" NOT NULL,
    phone character varying(20) COLLATE pg_catalog."default",
    date_registered TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT customer_pkey PRIMARY KEY (customer_id),
    CONSTRAINT customers_email_key UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS products
(
    product_id UUID NOT NULL,
    product_name character varying(40) COLLATE pg_catalog."default",
    quantity integer,
    price money,
    CONSTRAINT product_pkey PRIMARY KEY (product_id)
);

CREATE TABLE IF NOT EXISTS orders
(
    order_id integer NOT NULL GENERATED ALWAYS AS IDENTITY ( INCREMENT 1 START 1 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1 ,
    customer_id UUID NOT NULL,
    date_ordered timestamp with time zone,
    order_status ocharacter varying(10),
    CONSTRAINT orders_pkey PRIMARY KEY (order_id),
    CONSTRAINT orders_customerid_fkey FOREIGN KEY (customer_id)
        REFERENCES public.customers (customer_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

CREATE TABLE IF NOT EXISTS ordered_items
(
    item_id UUID NOT NULL,
    order_id integer,
    item_qty integer,
    product_id UUID,
    CONSTRAINT ordereditems_pkey PRIMARY KEY (item_id),
    CONSTRAINT ordereditems_orderid_fkey FOREIGN KEY (order_id)
        REFERENCES public.orders (order_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT ordereditems_productid_fkey FOREIGN KEY (product_id)
        REFERENCES public.products (product_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);
