drop table purchases;
drop table ads;
drop table offers;
drop table reviews;
drop table users;
drop table categories;

create table categories (
  cat 		char(10),
  supercat	char(10),
  primary key (cat),
  foreign key (supercat) references categories
);
create table users (
  email		char(20),
  name		char(20),
  pass		char(4),
  primary key (email)
);
create table reviews (
  rno		int,
  rating	int, 
  text		char(30),
  reviewer	char(20),
  reviewee	char(20),
  primary key (rno),
  foreign key (reviewer) references users,
  foreign key (reviewee) references users
);
create table offers (
  ono		int,
  ndays		int,
  price		float,
  primary key (ono)
);
create table ads (
  aid		char(4),
  atype		char(1)
    check (atype='S' OR atype='W'),	-- S (for sale), W (wanted)
  title		char(20),
  price		int,
  descr		char(40),
  location	char(15),
  pdate		date,
  cat		char(10),
  poster	char(20),
  primary key (aid),
  foreign key (cat) references categories,
  foreign key (poster) references users
);
create table purchases (
  pur_id	char(4),
  start_date	date,
  aid		char(4),
  ono		int,
  primary key (pur_id),
  foreign key (aid) references ads,
  foreign key (ono) references offers
);
