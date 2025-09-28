create table if not exists gutool_func
(
    id            integer
        constraint gutool_func_pk,
    name          text,
    define        text,
    content       text,
    remark        text,
    create_time   datetime,
    modified_time datetime
);

create table if not exists gutool_func_run_history
(
    id            integer
        constraint gutool_func_run_history_pk,
    func_id       integer,
    func_mirror   text,
    func_in       text,
    func_out      text,
    tab_panel_id integer,
    status        text,
    cost_time     integer,
    create_time   datetime,
    modified_time datetime
);
create index if not exists gutool_func_run_history_func_id_index
    on gutool_func_run_history (func_id);

create index if not exists gutool_func_run_history_tab_panel_id_index
    on gutool_func_run_history (tab_panel_id);


create table if not exists gutool_func_tab_panel
(
    id            integer
        constraint gutool_func_tab_panel_pk,
    name          text,
    define        text,
    remark        text,
    create_time   datetime,
    modified_time datetime
);

INSERT INTO gutool_func (id, name, define, content, remark, create_time, modified_time) VALUES (1971771263255187457, 'JSON格式化', null, 'import cn.hutool.json.JSONUtil;
return JSONUtil.toJsonPrettyStr(params);', null, '2025-09-27 10:57:57', '2025-09-27 11:09:15');
INSERT INTO gutool_func (id, name, define, content, remark, create_time, modified_time) VALUES (1971771317051330561, 'JSON压缩', null, 'import cn.hutool.json.JSONUtil;
return JSONUtil.toJsonStr(params);', null, '2025-09-27 10:58:10', '2025-09-27 11:06:03');

INSERT INTO gutool_func_tab_panel (id, name, define, remark, create_time, modified_time) VALUES (1971770743706828802, 'JSON', '{"type":"default","funcIn":"{\n\t\"aa\":\"bb\",\n\t\"CC\":123\n}","buttons":[{"type":"default","actionTriggerFuncId":1971771263255187457,"text":"格式化","toolTipText":"","order":0,"funOutMode":"输出结果"},{"type":"default","actionTriggerFuncId":1971771317051330561,"text":"压缩","toolTipText":"","order":1,"funOutMode":"输出结果"}],"outTextEnabled":true}', 'json 处理', '2025-09-27 10:55:53', '2025-09-27 11:05:26');




