$ErrorActionPreference='Stop'

function Invoke-Tsv([string]$sql){
  $raw = mysqlsh --sql -h localhost -P 3306 -u root -proot -D campus_activity_v2 -e $sql
  $lines = $raw -split "`r?`n" | Where-Object { $_ -and -not $_.StartsWith('WARNING:') }
  return (($lines -join "`n") | ConvertFrom-Csv -Delimiter "`t")
}

function SnakeToCamel([string]$name){
  $parts = $name -split '_'
  if($parts.Count -le 1){ return $name }
  $result = $parts[0]
  for($i=1; $i -lt $parts.Count; $i++){
    if($parts[$i].Length -gt 0){ $result += $parts[$i].Substring(0,1).ToUpper() + $parts[$i].Substring(1) }
  }
  return $result
}

function SqlTypeToJavaType([string]$table,[string]$column,[string]$columnType){
  $key = "$table.$column"
  $enumMap = @{
    'users.role'='UserRole';
    'users.status'='UserStatus';
    'organizer_profiles.organizer_type'='OrganizerType';
    'organizer_profiles.certification_status'='CertificationStatus';
    'activity_categories.status'='BasicStatus';
    'activities.status'='ActivityStatus';
    'activities.visibility'='Visibility';
    'activity_registrations.status'='RegistrationStatus';
    'activity_reviews.review_status'='ReviewStatus';
    'activity_audit_logs.action'='AuditAction';
    'auth_tokens.token_type'='TokenType';
    'login_logs.login_result'='LoginResult'
  }

  if($enumMap.ContainsKey($key)){ return $enumMap[$key] }
  if($columnType -eq 'tinyint(1)'){ return 'Boolean' }
  if($columnType -like 'tinyint*'){ return 'Integer' }
  if($columnType -like 'bigint*'){ return 'Long' }
  if($columnType -like 'int*'){ return 'Integer' }
  if($columnType -like 'datetime*'){ return 'LocalDateTime' }
  if($columnType -like 'text*' -or $columnType -like 'varchar*' -or $columnType -like 'char*'){ return 'String' }
  return 'String'
}

$entityMap = @{
  'users'='User';
  'student_profiles'='StudentProfile';
  'organizer_profiles'='OrganizerProfile';
  'activity_categories'='ActivityCategory';
  'activities'='Activity';
  'activity_category_rel'='ActivityCategoryRel';
  'activity_registrations'='ActivityRegistration';
  'activity_favorites'='ActivityFavorite';
  'organizer_follows'='OrganizerFollow';
  'activity_reviews'='ActivityReview';
  'activity_audit_logs'='ActivityAuditLog';
  'auth_tokens'='AuthToken';
  'login_logs'='LoginLog'
}

$methodMap = @{
  'users'=@('BaseMapper CRUD','selectByUsername(username)','selectByOpenid(openid)');
  'student_profiles'=@('BaseMapper CRUD');
  'organizer_profiles'=@('BaseMapper CRUD');
  'activity_categories'=@('BaseMapper CRUD','selectByParentId(parentId)');
  'activities'=@('BaseMapper CRUD','selectActivityList(status, visibility, organizerId, keyword, startFrom, startTo)');
  'activity_category_rel'=@('insertRelation(relation)','deleteByActivityIdAndCategoryId(activityId, categoryId)','selectByActivityIdAndCategoryId(activityId, categoryId)','selectCategoryLinksByActivityId(activityId)','selectActivityLinksByCategoryId(categoryId)');
  'activity_registrations'=@('BaseMapper CRUD','countEffectiveByActivityId(activityId)','countEffectiveByActivityIds(activityIds)','existsEffectiveRegistration(activityId, userId)');
  'activity_favorites'=@('BaseMapper CRUD','existsFavorite(activityId, userId)');
  'organizer_follows'=@('BaseMapper CRUD','existsFollow(organizerId, userId)');
  'activity_reviews'=@('BaseMapper CRUD','selectDetailByActivityId(activityId)');
  'activity_audit_logs'=@('BaseMapper CRUD','selectByActivityIdOrderByCreatedAtDesc(activityId)');
  'auth_tokens'=@('BaseMapper CRUD','selectValidTokensByUserId(userId, now)');
  'login_logs'=@('BaseMapper CRUD','selectRecentByUserId(userId, startTime, limit)')
}

$columns = Invoke-Tsv "SELECT TABLE_NAME, ORDINAL_POSITION, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, IFNULL(COLUMN_DEFAULT,'<NULL>') AS COLUMN_DEFAULT, IFNULL(EXTRA,'') AS EXTRA, IFNULL(COLUMN_KEY,'') AS COLUMN_KEY FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='campus_activity_v2' ORDER BY TABLE_NAME, ORDINAL_POSITION;"
$constraints = Invoke-Tsv "SELECT TABLE_NAME, CONSTRAINT_NAME, CONSTRAINT_TYPE FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA='campus_activity_v2' ORDER BY TABLE_NAME, CONSTRAINT_TYPE, CONSTRAINT_NAME;"
$indexes = Invoke-Tsv "SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS COLUMNS FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='campus_activity_v2' GROUP BY TABLE_NAME, INDEX_NAME, NON_UNIQUE ORDER BY TABLE_NAME, INDEX_NAME;"
$fks = Invoke-Tsv "SELECT CONSTRAINT_NAME, TABLE_NAME, REFERENCED_TABLE_NAME, UPDATE_RULE, DELETE_RULE FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA='campus_activity_v2' ORDER BY TABLE_NAME, CONSTRAINT_NAME;"
$fkCols = Invoke-Tsv "SELECT TABLE_NAME, CONSTRAINT_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA='campus_activity_v2' AND REFERENCED_TABLE_NAME IS NOT NULL ORDER BY TABLE_NAME, CONSTRAINT_NAME, ORDINAL_POSITION;"
$tableMeta = Invoke-Tsv "SELECT TABLE_NAME, TABLE_COMMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA='campus_activity_v2' ORDER BY TABLE_NAME;"

$sb = New-Object System.Text.StringBuilder
function AddLine([string]$line){ $null = $sb.AppendLine($line) }

AddLine '# campus_activity_v2 SQL映射说明（教学版，先结果后过程）'
AddLine ''
AddLine '说明：本文档仅作为 SQL 映射参考工具与学习手册，不作为执行脚本。'
AddLine '数据来源：实库 localhost:3306/campus_activity_v2（information_schema + SHOW/SELECT）。'
AddLine '安全边界：本次实现全程未执行 INSERT/UPDATE/DELETE/DDL，未修改数据库任何内容。'
AddLine ''
AddLine '## 一、最终产物总览（先看结果）'
AddLine ''
AddLine '- 后端路径：E:\Projects\WeiChatProjects\CampusActivity\backend'
AddLine '- 包名：com.campus.activity'
AddLine '- 工程栈：Spring Boot 3.x + MyBatis-Plus + Lombok + MySQL 8'
AddLine '- 数据层产物：13 个实体、13 个 Mapper 接口、13 份 XML、12 个枚举/状态类'
AddLine '- 已验证：mvn -q -DskipTests compile 与 mvn -q test 均通过'
AddLine ''
AddLine '### 1.1 表-实体-Mapper-XML 清单'
AddLine ''
AddLine '| 表名 | 实体类 | Mapper 接口 | XML 文件 |'
AddLine '|---|---|---|---|'
foreach($tm in $tableMeta){
  $t = $tm.TABLE_NAME
  $e = $entityMap[$t]
  AddLine("| $t | $e | ${e}Mapper | ${e}Mapper.xml |")
}

AddLine ''
AddLine '## 二、全局映射规则（统一规范）'
AddLine ''
AddLine '1. 命名规则：数据库下划线命名映射为 Java 驼峰命名。'
AddLine '2. 类型规则：DATETIME -> LocalDateTime，BIGINT -> Long，INT -> Integer。'
AddLine '3. 布尔规则：TINYINT(1) -> Boolean；users.gender 为 tinyint，保持 Integer。'
AddLine '4. 枚举规则：数据库 ENUM 统一映射为 Java enum，持久化按枚举名字符串。'
AddLine '5. 主键规则：自增主键 IdType.AUTO，非自增单主键 IdType.INPUT，复合主键表用自定义 SQL。'
AddLine '6. 初始化规则：spring.sql.init.mode=never，不引入迁移工具，不放 schema/data SQL。'
AddLine ''
AddLine '## 三、逐表映射明细（字段级）'

$tables = $tableMeta | ForEach-Object { $_.TABLE_NAME }
$tableNo = 0
foreach($table in $tables){
  $tableNo++
  $comment = ($tableMeta | Where-Object { $_.TABLE_NAME -eq $table } | Select-Object -First 1).TABLE_COMMENT
  $entity = $entityMap[$table]

  AddLine ''
  AddLine("### 3.$tableNo $table（$comment）")
  AddLine ''
  AddLine("- 实体：src/main/java/com/campus/activity/entity/$entity.java")
  AddLine("- Mapper：src/main/java/com/campus/activity/mapper/${entity}Mapper.java")
  AddLine("- XML：src/main/resources/mapper/${entity}Mapper.xml")
  AddLine ''
  AddLine '| 列名 | SQL 类型 | Java 字段 | Java 类型 | 可空 | 默认值 | 键类型 | 映射说明 |'
  AddLine '|---|---|---|---|---|---|---|---|'

  $cols = $columns | Where-Object { $_.TABLE_NAME -eq $table } | Sort-Object {[int]$_.ORDINAL_POSITION}
  foreach($c in $cols){
    $col = $c.COLUMN_NAME
    $javaField = SnakeToCamel $col
    if("$table.$col" -eq 'activities.is_featured'){ $javaField = 'featured' }

    $javaType = SqlTypeToJavaType -table $table -column $col -columnType $c.COLUMN_TYPE
    $note = '-'

    if($c.COLUMN_KEY -eq 'PRI' -and $c.EXTRA -match 'auto_increment'){
      $note = 'TableId AUTO'
    } elseif($c.COLUMN_KEY -eq 'PRI'){
      $note = 'TableId INPUT'
    }

    if("$table.$col" -eq 'activities.is_featured'){
      $note = 'TableField(is_featured)，避免 JavaBean is* 字段歧义'
    }

    if($javaType -match 'Status|Role|Type|Action|Visibility|Result'){
      if($note -eq '-'){
        $note = '枚举映射（字符串持久化）'
      } else {
        $note = "$note；枚举映射（字符串持久化）"
      }
    }

    if($table -eq 'activity_category_rel' -and $c.COLUMN_KEY -eq 'PRI'){
      $note = '复合主键列，配合自定义 Mapper 条件方法使用'
    }

    AddLine("| $col | $($c.COLUMN_TYPE) | $javaField | $javaType | $($c.IS_NULLABLE) | $($c.COLUMN_DEFAULT) | $($c.COLUMN_KEY) | $note |")
  }

  AddLine ''
  AddLine '- 约束/索引/外键：'
  $tableCons = $constraints | Where-Object { $_.TABLE_NAME -eq $table }
  foreach($tc in $tableCons){
    AddLine("  - $($tc.CONSTRAINT_TYPE)：$($tc.CONSTRAINT_NAME)")
  }

  $tableIdx = $indexes | Where-Object { $_.TABLE_NAME -eq $table }
  foreach($ix in $tableIdx){
    $uniq = if($ix.NON_UNIQUE -eq '0'){'UNIQUE'} else {'NON_UNIQUE'}
    AddLine("  - 索引 $($ix.INDEX_NAME)（$uniq）：$($ix.COLUMNS)")
  }

  $tableFks = $fks | Where-Object { $_.TABLE_NAME -eq $table }
  foreach($fk in $tableFks){
    $pairs = ($fkCols | Where-Object { $_.TABLE_NAME -eq $table -and $_.CONSTRAINT_NAME -eq $fk.CONSTRAINT_NAME } | ForEach-Object { "$($_.COLUMN_NAME) -> $($_.REFERENCED_TABLE_NAME).$($_.REFERENCED_COLUMN_NAME)" }) -join ', '
    AddLine("  - 外键 $($fk.CONSTRAINT_NAME)：$pairs；UPDATE $($fk.UPDATE_RULE)，DELETE $($fk.DELETE_RULE)")
  }

  AddLine ''
  AddLine '- Mapper 方法：'
  foreach($m in $methodMap[$table]){ AddLine("  - $m") }

  AddLine ''
  AddLine '- 代码注释重点：'
  switch ($table) {
    'users' {
      AddLine('  - gender 注释明确 0/1/2 语义，避免把 tinyint 当布尔。')
      AddLine('  - forcePasswordChange 注释明确为首次登录安全策略。')
    }
    'activities' {
      AddLine('  - featured 通过 TableField(is_featured) 显式绑定原字段。')
      AddLine('  - registeredCount 注释标识其为缓存计数字段，明细以报名表为准。')
    }
    'activity_category_rel' {
      AddLine('  - 类注释明确复合主键，不使用 updateById/deleteById。')
    }
    'activity_registrations' {
      AddLine('  - SQL 注释写明“有效报名”判定仅含 REGISTERED/CHECKED_IN。')
    }
    'activity_reviews' {
      AddLine('  - selectDetailByActivityId 注释解释为何使用 LEFT JOIN（reviewer 可空）。')
    }
    'auth_tokens' {
      AddLine('  - selectValidTokensByUserId 注释明确 revoked=0 + expires_at>now 双条件。')
    }
    'login_logs' {
      AddLine('  - selectRecentByUserId 注释强调必须带 LIMIT 防止日志表大查询。')
    }
    default {
      AddLine('  - 注释聚焦字段语义、索引意图、边界行为，便于后续扩展查询。')
    }
  }
}

AddLine ''
AddLine '## 四、关键 SQL 注释示例（教学版）'
AddLine ''
AddLine '### 4.1 活动列表（多条件 + 关联信息）'
AddLine '```sql'
AddLine '-- 目标：同一查询返回活动基础信息、组织者名称、审核状态、分类聚合字符串'
AddLine '-- 索引：idx_activities_status / idx_activities_organizer / idx_activities_start_time'
AddLine 'SELECT a.id, a.title, a.start_time, a.status, ar.review_status, op.organizer_name,'
AddLine "       GROUP_CONCAT(DISTINCT ac.name ORDER BY ac.name SEPARATOR ',') AS category_names"
AddLine 'FROM activities a'
AddLine 'LEFT JOIN organizer_profiles op ON op.user_id = a.organizer_id'
AddLine 'LEFT JOIN activity_reviews ar ON ar.activity_id = a.id'
AddLine 'LEFT JOIN activity_category_rel acr ON acr.activity_id = a.id'
AddLine 'LEFT JOIN activity_categories ac ON ac.id = acr.category_id'
AddLine '-- WHERE 使用 MyBatis 动态条件：status/visibility/organizerId/keyword/startFrom/startTo'
AddLine 'GROUP BY a.id, ar.review_status, op.organizer_name'
AddLine 'ORDER BY a.start_time DESC, a.id DESC;'
AddLine '```'
AddLine ''
AddLine '### 4.2 报名统计（有效状态定义）'
AddLine '```sql'
AddLine '-- 目标：统计有效报名，不把取消和候补算入占用名额'
AddLine 'SELECT COUNT(1)'
AddLine 'FROM activity_registrations'
AddLine 'WHERE activity_id = #{activityId}'
AddLine "  AND status IN ('REGISTERED', 'CHECKED_IN');"
AddLine '```'
AddLine ''
AddLine '### 4.3 收藏/关注按钮状态'
AddLine '```sql'
AddLine '-- 目标：返回布尔值，减少上层二次判断逻辑'
AddLine 'SELECT CASE WHEN COUNT(1) > 0 THEN TRUE ELSE FALSE END'
AddLine 'FROM activity_favorites'
AddLine 'WHERE activity_id = #{activityId} AND user_id = #{userId};'
AddLine '```'
AddLine ''
AddLine '### 4.4 复合主键精确查询'
AddLine '```sql'
AddLine '-- 目标：活动-分类关系通过 activity_id + category_id 精确定位'
AddLine 'SELECT activity_id, category_id, created_at'
AddLine 'FROM activity_category_rel'
AddLine 'WHERE activity_id = #{activityId} AND category_id = #{categoryId};'
AddLine '```'

AddLine ''
AddLine '## 五、编写过程（从结果回看过程）'
AddLine ''
AddLine '1. 从实库拉取元数据（表、列、主键、唯一键、索引、外键规则）。'
AddLine '2. 固化全局映射规则（类型、枚举、主键策略、初始化策略）。'
AddLine '3. 先写实体与枚举，解决字段语义、命名冲突、复合主键难点。'
AddLine '4. 先定义 Mapper 方法签名，再实现 XML，保证参数和 SQL 一致。'
AddLine '5. 对每条关键 SQL 补“用途 + 索引命中 + 注意点”注释。'
AddLine '6. 用 mvn compile 与 mvn test 验证映射可加载，再做只读 SQL 校验。'

AddLine ''
AddLine '## 六、常见问题与排查建议'
AddLine ''
AddLine '- 复合主键误当单主键：activity_category_rel 必须双条件。'
AddLine '- ENUM 不一致：Java 枚举值必须与 MySQL ENUM 字面量完全一致。'
AddLine '- is_featured 命名冲突：用 featured + TableField(is_featured)。'
AddLine '- 时区偏移：JDBC URL 指定 serverTimezone=Asia/Shanghai。'
AddLine '- 外键删除策略误判：按实库区分 CASCADE/SET NULL/RESTRICT。'

AddLine ''
AddLine '## 七、只读验证记录'
AddLine ''
AddLine '- 数据库版本：MySQL 8.0.45'
AddLine '- 实库表数量：13'
AddLine '- 编译校验：通过'
AddLine '- Spring + MyBatis 映射加载测试：通过'
AddLine '- 数据库写操作：未执行'

$target = 'C:\Users\Administrator\Desktop\毕业设计\SQL映射.md'
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($target, $sb.ToString(), $utf8NoBom)
Write-Output "WROTE: $target"
