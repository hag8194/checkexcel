create table `countries` (
`code` VARCHAR(255) NOT NULL,
`name` VARCHAR(255) NOT NULL,
primary key (`code`));

create table `states` (
`id` int(11) not null,
`name`varchar(255),
primary key (`id`));

create table `cites`(
`name`varchar (255) not null,
`state_id` int(11) not null,
primary key(`name`));