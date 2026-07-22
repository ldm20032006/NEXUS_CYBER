alter table lobbies
    add column voice_provider varchar(50),
    add column voice_channel_id varchar(150),
    add column voice_status varchar(30) not null default 'DISABLED';

create index idx_lobbies_voice_channel on lobbies (voice_provider, voice_channel_id);
