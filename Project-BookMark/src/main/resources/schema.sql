DROP TABLE IF EXISTS bookmarks;
create table bookmarks(
  id         bigserial primary key,
  title      varchar not null,
  url        varchar not null,
  created_at timestamp,
  updated_at timestamp,
  created_by varchar,
  updated_by varchar
);

--DROP TABLE IF EXISTS bookmarks;
--    create TABLE bookmarks (
--    id bigserial PRIMARY KEY,
--    title VARCHAR NOT NULL,
--    url varchar NOT NULL,
--    created_at timestamp,
--    updated_at timestamp
--);
