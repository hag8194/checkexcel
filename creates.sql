create table `countries` (
`code` VARCHAR(255) NOT NULL,
`name` text(255) NOT NULL);

create table `states` (
`id` int(11) not null,
`name`text(255),
`country_code` varchar(255));

create table `cities`(
`name`text (255) not null,
`state_id` int(11) not null);