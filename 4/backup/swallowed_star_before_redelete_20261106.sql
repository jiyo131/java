-- MySQL dump 10.13  Distrib 8.0.39, for Win64 (x86_64)
--
-- Host: localhost    Database: swallowed_star
-- ------------------------------------------------------
-- Server version	8.0.39

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `star_category`
--

DROP TABLE IF EXISTS `star_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `star_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父分类 id，0 表示一级分类',
  `code` varchar(48) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分类编码（唯一，便于前端/脚本引用）',
  `name` varchar(48) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分类名称',
  `level` tinyint NOT NULL DEFAULT '1' COMMENT '层级：1 一级 / 2 二级 / …',
  `icon` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分类图标（emoji）',
  `color` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分类主色（#RRGGBB），驱动前端角标与卡片光效',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分类说明',
  `sort_no` int NOT NULL DEFAULT '0' COMMENT '同级排序（小值优先）',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用 / 0 停用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_code` (`code`) USING BTREE,
  KEY `idx_parent` (`parent_id`) USING BTREE,
  KEY `idx_level_sort` (`level`,`sort_no`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='涂山档案分类表（多级）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `star_category`
--

LOCK TABLES `star_category` WRITE;
/*!40000 ALTER TABLE `star_category` DISABLE KEYS */;
INSERT INTO `star_category` VALUES (1,0,'RENWU','人物档案',1,'🧑','#ff9eb5','涂山与人妖两界有名有姓的众生相',10,1,'2026-10-23 16:36:34'),(2,1,'RW_TUSHAN','涂山一脉',2,'🦊','#ff9eb5','涂山狐妖一族与红线仙',10,1,'2026-10-23 16:36:34'),(3,1,'RW_DAOMEN','道门修士',2,'🗡','#ffc2cf','一气道盟与各大家族的修行者',20,1,'2026-10-23 16:36:34'),(4,1,'RW_YAOZU','妖族众生',2,'🌸','#ff5c7a','散落两界、各有执念的妖',30,1,'2026-10-23 16:36:34'),(5,0,'SHENGDI','圣地秘境',1,'🏞','#e09bff','承载红线与前世记忆的地方',20,1,'2026-10-23 16:36:34'),(6,5,'SD_TUSHAN','涂山圣地',2,'🌳','#e09bff','苦情树所在的涂山核心',10,1,'2026-10-23 16:36:34'),(7,5,'SD_AOLAI','傲来秘境',2,'🌊','#c9b6ff','东海之上的傲来国',20,1,'2026-10-23 16:36:34'),(8,0,'FABAO','法宝器物',1,'🪭','#ffd479','红娘行头与两界神兵',30,1,'2026-10-23 16:36:34'),(9,8,'FB_HONGNIANG','红娘法器',2,'🧵','#ffd479','牵线续缘所用的器物',10,1,'2026-10-23 16:36:34'),(10,8,'FB_SHENBING','神兵符箓',2,'⚔','#ffb84d','两界闻名的兵器与符箓',20,1,'2026-10-23 16:36:34'),(11,0,'ZUZHI','两界组织',1,'🏮','#7fe7c9','维系两界秩序的势力与盟约',40,1,'2026-10-23 16:36:34'),(12,11,'ZZ_TUSHAN','涂山势力',2,'🏯','#7fe7c9','以涂山为核心的妖族盟约',10,1,'2026-10-23 16:36:34'),(13,11,'ZZ_DAOMEN','道盟势力',2,'🏛','#9fe8d6','一气道盟及其下属机构',20,1,'2026-10-23 16:36:34');
/*!40000 ALTER TABLE `star_category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `star_entry`
--

DROP TABLE IF EXISTS `star_entry`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `star_entry` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category_id` bigint NOT NULL COMMENT '所属分类 id（一般挂在二级分类上）',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `alias` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '称号 / 别名',
  `glyph` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '卡片图标（emoji 或单字），avatar 为空时回退使用',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '人物形象图（前端 /avatars/ 下的图片路径）',
  `intro` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '简介',
  `quote` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '经典台词',
  `bond` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '红线羁绊',
  `faction` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '所属势力',
  `tags` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关键词标签，英文逗号分隔',
  `hot_score` int NOT NULL DEFAULT '0' COMMENT '热度权重（编辑推荐）',
  `view_count` int NOT NULL DEFAULT '0' COMMENT '浏览量（详情页累加）',
  `sort_no` int NOT NULL DEFAULT '0' COMMENT '默认排序（小值优先）',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 上架 / 0 下架',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_name` (`name`) USING BTREE,
  KEY `idx_category` (`category_id`,`status`,`sort_no`) USING BTREE,
  KEY `idx_hot` (`status`,`hot_score`,`view_count`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='涂山档案条目表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `star_entry`
--

LOCK TABLES `star_entry` WRITE;
/*!40000 ALTER TABLE `star_entry` DISABLE KEYS */;
INSERT INTO `star_entry` VALUES (1,2,'涂山苏苏','涂山三小姐 · 红线仙','🦊','/avatars/tushan-susu.jpg','涂山三姐妹中的小妹，一只通体赤红的九尾小狐妖。天性呆萌、爱吃爱闹，却立志成为世上最出色的红娘，与白月初结伴游走人妖两界，为人续那一世未了之缘。','「苏苏一定会成为最棒的红娘哒！」','白月初 —— 契约红线仙搭档，一妖一道，互为软肋','涂山','呆萌,红线仙,续缘术',100,1287,10,1,'2026-10-23 16:36:34','2026-10-23 16:38:28'),(2,3,'白月初','一气道盟 · 天才道士','🗡','/avatars/baiyuechu.jpg','一气道盟的后起之秀，毒舌傲娇、心口不一，嘴上说着不管，却总在关键时刻挡在苏苏身前。符箓剑意纵横，承接两界的续缘委托。','「我才不是为了你才留下来的！」','涂山苏苏 —— 契约搭档，斗嘴一生，护她一生','一气道盟','毒舌,符箓,契约',96,1104,20,1,'2026-10-23 16:36:34','2026-10-23 16:36:34'),(3,2,'涂山红红','涂山之主 · 妖盟盟主','👑','/avatars/tushan-honghong.jpg','涂山至高守护者，九尾天狐，亦是妖盟盟主。以一人之力镇守人妖两界的平衡，是所有妖族心中不可撼动的传说，也是红线一脉真正的源头。','「只要我还在，涂山就不会倒。」','东方月初 —— 跨越前世今生的未了之约','涂山','九尾天狐,妖盟盟主,涂山之主',99,1532,30,1,'2026-10-23 16:36:34','2026-10-23 16:36:34'),(4,3,'东方月初','傲来国三少','🌙','/avatars/dongfang-yuechu.jpg','白月初的前世，傲来国三少，天赋绝伦却命途多舛。与涂山红红的相遇，写下了一段被红线反复重提的宿命。','「这一世，我还是要站在你身边。」','涂山红红 —— 前世之约，红线另一端的执念','傲来国','前世,傲来国,宿命',92,986,40,1,'2026-10-23 16:36:34','2026-10-23 16:36:34'),(5,3,'王权富贵','王权家 · 道门兵人','⚔','/avatars/wangquan-fugui.jpg','王权家倾全族之力培养的天才兵人，剑法凌厉、心性却极静。因清瞳一句「我愿意」，第一次违逆家族意志，令两界为之动容。','「我愿意。」','清瞳 —— 一句承诺，牵动两界','王权家','王家剑法,兵人,觉醒',95,1203,50,1,'2026-10-23 16:36:34','2026-10-23 16:36:34'),(6,4,'清瞳','蛛妖 · 织梦者','🕸','/avatars/qingtong.jpg','温柔的蜘蛛精少女，被王权富贵所救。以丝为笔，为他织出「千里眼」，也织出了一段被红线铭记的深情。','「我织给你的，是整个世界。」','王权富贵 —— 以丝为绳，系住彼此的命','独立','蛛丝,千里眼,温柔',90,942,60,1,'2026-10-23 16:36:34','2026-10-23 16:36:34'),(7,2,'涂山雅雅','涂山二姐 · 冰系大师','❄','/avatars/tushan-yaya.jpg','涂山二小姐，冰系法术登峰造极，外冷内热。嘴上嫌弃苏苏，行动上却永远是第一个冲出来护短的姐姐。','「谁敢动我妹妹，先问过我的冰。」','涂山苏苏 —— 表面嫌弃、实则宠溺的姐姐线','涂山','冰系,傲娇,护短',88,876,70,1,'2026-10-23 16:36:34','2026-10-23 16:36:34'),(8,2,'涂山容容','涂山大姐 · 首席智囊','📖','/avatars/tushan-rongrong.jpg','涂山大小姐，掌管涂山一切大小事务，精于算计与经营。看似温婉柔和，实则握着整个涂山的情报网与钱袋。','「涂山从不做亏本的姻缘。」','涂山姐妹 —— 一个家族的定海神针','涂山','智谋,掌事,情报',86,803,80,1,'2026-10-23 16:36:34','2026-10-23 16:36:34'),(9,3,'王权霸业','王权家家主','🛡','/avatars/wangquan-baye.jpg','王权家家主，强者为尊的践行者。一手托起王权家的荣光，也亲手将儿子推入了冰冷的宿命，是父亲，更是家主。','「王权家的儿子，没有选择。」','王权富贵 —— 父子之间最沉重的那把剑','王权家','家主,威严,家族',78,612,90,1,'2026-10-23 16:36:34','2026-10-23 16:36:34'),(10,6,'苦情树','涂山圣地 · 红线之源','🌸','/avatars/kuqing-shu.jpg','涂山圣地，立于树下便可一窥前世。所有红线的起点与终点，红娘续缘的力量皆源自于此，人妖两界的人都愿来此许一个来生。','「树在，缘就在。」','所有红线 —— 缘起缘落，皆由此树见证','涂山','圣地,前世记忆,续缘',94,1370,100,1,'2026-10-23 16:36:34','2026-10-23 16:36:34'),(11,9,'忆梦锤','红娘法器','🔨','/avatars/yimenghui.jpg','红娘随身法器，轻敲便可唤醒续缘者前世的记忆碎片，是牵线续缘中不可或缺的一环。','（一记闷响，前世便回来了）','涂山苏苏 —— 红线仙的标配行头','涂山','法器,记忆,牵线',70,486,110,1,'2026-10-23 16:36:34','2026-10-23 16:36:34'),(12,9,'红线','姻缘羁绊 · 命定之丝','🧵','/avatars/hongxian.jpg','涂山红娘所牵引的命定丝线。一旦系上，纵隔两界轮回，缘分终会重逢，这是涂山最强的执念，也是最温柔的力量。','「红线不会断，只会等你。」','人 · 妖 · 两界 —— 所有故事的开端','涂山','羁绊,命定,轮回',98,1655,120,1,'2026-10-23 16:36:34','2026-10-23 16:36:34'),(13,13,'一气道盟','两界共尊的道门联盟','🏯','/avatars/yiqidaomeng.jpg','人妖两界共尊的道门联盟，主持公道、缉拿妖邪，是维系两界秩序的中坚力量，也是白月初出身的势力。','「道在心，不在门派。」','白月初 · 涂山 —— 时而对峙，时而并肩','一气道盟','道门,秩序,联盟',82,655,130,1,'2026-10-23 16:36:34','2026-10-23 16:36:34'),(14,12,'妖盟','涂山主导 · 群妖共主','🏮','/avatars/yaomeng.jpg','由涂山主导的妖族联盟，涂山红红任盟主，统领群妖。与一气道盟一同守护两界的脆弱平衡。','「妖亦有道。」','涂山红红 —— 盟主之位，亦是万钧之责','涂山','妖族,盟约,共主',80,598,140,1,'2026-10-23 16:36:34','2026-10-23 16:36:34'),(15,3,'白裘恩','白月初之父 · 一气道盟长老','🍶','/avatars/baiqien.jpg','白月初的父亲，看似嗜酒贪财、吊儿郎当，实则深藏不露。嘴上总把儿子挂在嘴边，关键时刻却比谁都靠得住。','「儿子，爹这是为你好。」','白月初 —— 一对互相嫌弃又彼此牵挂的父子','一气道盟','长老,深藏不露,父子',74,512,150,1,'2026-10-23 16:36:34','2026-10-23 16:36:34'),(16,4,'石宽','北山妖帝','🗿','/avatars/shikuan.jpg','北山之主，妖族的一方帝王。外表粗豪、心思却极细腻，为了心中所念，甘愿把整个北山都押上去。','「北山，从不欠人。」','欢都落兰 —— 隔着毒与火的执念','北山','妖帝,北山,痴情',76,540,160,1,'2026-10-23 16:36:34','2026-10-23 16:36:34'),(17,4,'欢都落兰','南国公主 · 毒术大家','🦋','/avatars/huandu-luolan.jpg','南国公主，毒术出神入化，性子骄纵却不失真心。一身毒，一身傲，遇上了便再不肯退。','「本公主的东西，谁也别想抢。」','石宽 —— 一个愿打，一个愿挨','南国','毒术,公主,傲娇',73,498,170,1,'2026-10-23 16:36:34','2026-10-23 16:36:34'),(18,3,'东方淮竹','东方世家 · 王权富贵之母','🎋','/avatars/dongfang-huaizhu.jpg','东方世家之女，气质清冷、灵力精纯。与王权霸业的结合始于家族，却在冷硬的规矩里留下了一点温度，也是王权富贵性情的来处。','「这孩子，不该只是一把剑。」','王权富贵 —— 母亲留下的那一点柔软','东方世家','东方世家,母亲,灵力',68,402,180,1,'2026-10-23 16:36:34','2026-10-23 16:36:34'),(19,3,'东方秦兰','东方世家 · 东方月初之母','🌺','/avatars/dongfang-qinlan.jpg','东方世家的女儿，东方淮竹之妹，东方月初的母亲。性烈如火、护子心切，把东方一脉最后的血脉护到了最后。','「活下去，比什么都重要。」','东方月初 —— 母亲用命换来的那一线生机','东方世家','东方世家,母亲,护子',66,388,190,1,'2026-10-23 16:36:34','2026-10-23 16:36:34'),(20,3,'律笺文','一气道盟 · 执法者','📜','/avatars/lvjianwen.jpg','一气道盟的执法者，铁面无私、只认规矩不认人情。手持律令行走两界，是与非在她这里从没有模糊地带。','「律法面前，人妖同罪。」','一气道盟 —— 规矩的化身','一气道盟','执法,律令,铁面',64,356,200,1,'2026-10-23 16:36:34','2026-10-23 16:36:34'),(21,7,'傲来国','东海之上 · 三少故里','🌊','/avatars/aolai-guo.jpg','东海之上的神秘国度，物产丰饶、灵气充沛，东方月初的出身之地。三少之名传遍两界，也让这个国度始终笼罩在传说里。','「东海的水，记着所有来过的人。」','东方月初 —— 出走半生，故国仍在','傲来国','东海,秘境,三少',69,431,210,1,'2026-10-23 16:36:34','2026-10-23 16:36:34'),(22,10,'王权剑','王权家传世名剑','🗡','/avatars/wangquan-jian.jpg','王权家世代相传的名剑，剑意与持有者的心性相通。它曾斩过妖，也曾因一句「我愿意」停在半空，是两界最锋利也最沉重的兵器。','「剑在人在，剑断人亡。」','王权富贵 —— 一把剑与一个兵人','王权家','名剑,家传,剑意',72,466,220,1,'2026-10-23 16:36:34','2026-10-23 16:36:34'),(23,3,'王权醉','王权家小妹','🎐','/avatars/wangquan-zui.jpg','王权家的小女儿，王权富贵的妹妹。性子活泼娇蛮，一身剑术却货真价实，是王权家冷硬规矩里少有的一点热闹。','「我哥不行，还有我呢！」','王权富贵 —— 兄妹之间，一个冷一个闹','王权家','王权家,小妹,剑术',62,342,230,1,'2026-10-23 16:36:34','2026-10-23 16:36:34'),(24,4,'颜如玉','千颜 · 画皮之妖','🎭','/avatars/yan-ruyu.jpg','能变化万千面容的画皮之妖，游走两界只为寻一个「真实的自己」。与律笺文的相遇，让他第一次想以真面目示人。','「这一次，我想用真的脸见你。」','律笺文 —— 一张脸换一颗心','妖族','画皮,千颜,变脸',71,520,240,1,'2026-10-23 16:36:34','2026-10-23 16:36:34'),(25,4,'欢都擎天','南国之主','🐉','/avatars/huandu-qingtian.jpg','南国之主，欢都落兰的父亲。一身蛊毒之术冠绝两界，护短护到没边，是南国这一方毒泽里说一不二的存在。','「南国的规矩，我说了算。」','欢都落兰 —— 女儿是他的软肋','南国','南国,蛊毒,王',60,298,250,1,'2026-10-23 16:36:34','2026-10-23 16:36:34'),(26,6,'涂山城','涂山脚下的狐族城池','🏯','/avatars/tushan-cheng.jpg','涂山脚下的狐族城池，街市昼夜不歇，人妖在此以物易物、以心换心。红娘事务所的招牌，就挂在城中最热闹的那条街上。','「进了涂山城，就是一家人。」','涂山 —— 城在山下，缘在城中','涂山','城池,市集,红娘',65,410,260,1,'2026-10-23 16:36:34','2026-10-23 16:36:34'),(27,10,'千里眼','蛛丝织成的眼睛','👁','/avatars/qianliyan.jpg','清瞳以蛛丝亲手织成的「千里眼」，戴上便可望穿千里。礼轻情重，一根丝里织着的是说不出口的心意。','（丝线闪烁，如星子落进眼底）','王权富贵 —— 以丝为目，代你看尽山河','王权家','蛛丝,千里眼,信物',63,336,270,1,'2026-10-23 16:36:34','2026-10-23 16:36:34'),(28,9,'姻缘簿','涂山红娘的册命簿','📕','/avatars/yinyuanbu.jpg','涂山红娘的册命簿，天下姻缘皆记于其上。名字落笔便是有缘，红线一牵，纵隔轮回也要重逢。','「簿上无名？那就现在添上。」','涂山红娘 —— 一笔一画，皆是执念','涂山','册命,姻缘,红娘',75,618,280,1,'2026-10-23 16:36:34','2026-10-23 16:36:34'),(29,12,'妖馨斋','涂山脚下的食肆','🍵','/avatars/yaoxinzhai.jpg','涂山脚下最有名的食肆，也是苏苏的心头好。饭菜香混着人情味，两界的消息随茶水流转——这是容容的另一门生意。','「客官，要加一份今日消息吗？」','涂山容容 —— 饭桌之上，情报满桌','涂山','食肆,情报,烟火气',58,286,290,1,'2026-10-23 16:36:34','2026-10-23 16:36:34'),(30,9,'红线仙令','红线仙出任务凭证','🎴','/avatars/hongxianling.jpg','红线仙出任务的凭证，令至如人至。令牌一出，无论人妖两界，接了委托的红娘都必须亲自到场，这是涂山的信誉。','「令在人在，缘不迟到。」','涂山苏苏 —— 第一次独自出任务的行头','涂山','令牌,凭证,出任务',56,265,300,1,'2026-10-23 16:36:34','2026-10-23 16:36:34');
/*!40000 ALTER TABLE `star_entry` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `star_redline_task`
--

DROP TABLE IF EXISTS `star_redline_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `star_redline_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` varchar(48) COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务编码（唯一，便于脚本引用）',
  `title` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务名称',
  `subtitle` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '副标题 / 一句话说明',
  `description` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '委托详情',
  `client` varchar(48) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '委托方',
  `glyph` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '任务图标（emoji）',
  `color` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '任务主色（#RRGGBB）',
  `difficulty` tinyint NOT NULL DEFAULT '1' COMMENT '难度：1 轻缘 / 2 深缘 / 3 宿缘',
  `reward` int NOT NULL DEFAULT '10' COMMENT '完成可得情绪值',
  `duration` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '时限描述',
  `scene` varchar(48) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '委托地点',
  `tags` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关键词标签，英文逗号分隔',
  `sort_no` int NOT NULL DEFAULT '0' COMMENT '排序（小值优先）',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 可接取 / 0 下架',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_code` (`code`),
  KEY `idx_task_status` (`status`,`sort_no`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='红线仙任务告示板';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `star_redline_task`
--

LOCK TABLES `star_redline_task` WRITE;
/*!40000 ALTER TABLE `star_redline_task` DISABLE KEYS */;
INSERT INTO `star_redline_task` VALUES (1,'RL-001','苦情树下的第一卦','替一只迷路的小狐问一句姻缘','有只刚化形的小狐妖在苦情树下转了三天，说什么也不肯开口问卦。她不缺姻缘，只缺一个敢替她问的人。带上红线仙令，替她摇一卦吧。','涂山苏苏','🦊','#ff9eb5',1,12,'当日可成','苦情树下','姻缘,初见,问卦',10,1,'2026-11-06 15:42:55'),(2,'RL-002','忘忧酒馆的旧账','找回被撕走的那页红线记录','忘忧酒馆的账本少了一页，上面记着一对夫妻的续缘凭证。容容说，账可以糊涂，缘不能。去酒馆后厨的酒坛堆里翻一翻。','涂山容容','🍶','#e09bff',2,22,'限一日','忘忧酒馆','记录,酒馆,旧账',20,1,'2026-11-06 15:42:55'),(3,'RL-003','千年前的半截红线','把断在轮回里的红线重新续上','越山的那根红线只牵了一半，另一端在千年前就断了。要续上它，得先找到当年那个人留在苦情树根下的半枚铜钱。','涂山红红','🧵','#ff5c7a',3,40,'三日为期','苦情树根','续缘,前世,红线',30,1,'2026-11-06 15:42:55'),(4,'RL-004','白月初的符箓残页','追回被山风吹散的三张符纸','某人画符时打了个喷嚏，三张新符被山风卷走，散在一气道盟的山道上。他嘴上说不必管，符却少一张都画不成阵。','白月初','🗡','#ffc2cf',1,15,'半日','一气道盟山门','符箓,跑腿,叽叽喳喳',40,1,'2026-11-06 15:42:55'),(5,'RL-005','红线仙的入门考核','为一对别扭的小情侣牵一次线','雅雅设的考核：城南那对小情侣明明互相惦记，却谁都不肯先低头。不许用法术，只许用嘴和腿，把他们牵到苦情树下。','涂山雅雅','🎴','#ffd479',2,28,'两日','涂山街头','考核,牵线,实战',50,1,'2026-11-06 15:42:55'),(6,'RL-006','傲来国潮汐之约','赶在潮水退去前把信送到','东海傲来国，只有落潮那一刻礁石路才会浮出来。信要在潮水退尽之前交到岸边等了三年的那个人手里，误了时辰，路就没了。','东方月初','🌊','#7fe7c9',3,45,'潮退为限','傲来国海岸','送信,潮汐,约定',60,1,'2026-11-06 15:42:55'),(7,'RL-007','情蛊的解药','替一位不敢开口的姑娘解开心结','一位姑娘中了情蛊，蛊不伤人，只让她每夜梦见同一个人。解药不在药庐，在她自己那句始终没说出口的话里。','无名小妖','🪷','#ff9eb5',2,26,'三日','涂山药庐','心结,情蛊,解药',70,1,'2026-11-06 15:42:55'),(8,'RL-008','涂山夜巡','巡一遍山门，把闲散的执念送回原处','入夜后的涂山最热闹，也最容易出事。提着灯走一圈山门，遇着不肯走的执念，就替它们指一条回去的路。','涂山守卫','🏮','#f7b26b',1,18,'入夜起更','涂山山门','夜巡,守山,日常',80,1,'2026-11-06 15:42:55'),(9,'RL-009','一气道盟的和谈书','把涂山的和谈书送到道盟手上','两界又起了些摩擦，涂山备下和谈书。路上不太平，送信的人得让道盟明白：这封信不是求和，是给两界都留一条退路。','涂山容容','📜','#9fe8d6',2,30,'限两日','一气道盟','送信,和谈,两界',90,1,'2026-11-06 15:42:55'),(10,'RL-010','写给前世的一封信','替红线仙写一封不必寄出的信','苦情树下压着一封没写完的信，写信的人已经转世，忘了自己写过。红线仙的任务很简单：替他把最后一句补上。写完，情绪值最高。','苦情树','✉️','#ffd9a0',3,50,'不限时','苦情树下','心事,前世,执笔',100,1,'2026-11-06 15:42:55');
/*!40000 ALTER TABLE `star_redline_task` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `star_search_log`
--

DROP TABLE IF EXISTS `star_search_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `star_search_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `keyword` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '检索词',
  `user_id` bigint DEFAULT NULL COMMENT '检索用户 id（未登录为 NULL）',
  `result_count` int NOT NULL DEFAULT '0' COMMENT '命中条数（用于分析零结果词）',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '检索时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_keyword` (`keyword`) USING BTREE,
  KEY `idx_created` (`created_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='涂山档案检索日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `star_search_log`
--

LOCK TABLES `star_search_log` WRITE;
/*!40000 ALTER TABLE `star_search_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `star_search_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `star_task_record`
--

DROP TABLE IF EXISTS `star_task_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `star_task_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '接取人 id',
  `task_id` bigint NOT NULL COMMENT '任务 id',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1 进行中 / 2 已圆满',
  `accepted_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '接取时间',
  `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_task` (`user_id`,`task_id`),
  KEY `idx_record_user` (`user_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='红线仙任务接取记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `star_task_record`
--

LOCK TABLES `star_task_record` WRITE;
/*!40000 ALTER TABLE `star_task_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `star_task_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `star_user`
--

DROP TABLE IF EXISTS `star_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `star_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `username` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '璐﹀彿',
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '瀵嗙爜锛圔Crypt 鍔犲瘑瀛樺偍锛',
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '鏄电О',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 正常 / 0 停用（停用后无法登录）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_username` (`username`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='娑傚北绾㈢嚎鐢ㄦ埛琛';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `star_user`
--

LOCK TABLES `star_user` WRITE;
/*!40000 ALTER TABLE `star_user` DISABLE KEYS */;
INSERT INTO `star_user` VALUES (1,'admin','$2a$10$pKp09qk4v5F0l2fOctwIju6OkhNkO.6vXq.WODBvKX8yVZH1WzKoy','涂山红娘','2026-10-23 15:34:46',1),(2,'jwy','$2a$10$EUKQonG0VR6x.u0dKfQe2.FXLNCYLfXf3aQ/alAQj4so5DA/fSjQa','jwy','2026-10-23 15:35:49',1);
/*!40000 ALTER TABLE `star_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `star_user_emotion`
--

DROP TABLE IF EXISTS `star_user_emotion`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `star_user_emotion` (
  `user_id` bigint NOT NULL COMMENT '用户 id（主键）',
  `emotion` int NOT NULL DEFAULT '0' COMMENT '当前情绪值',
  `total_gain` int NOT NULL DEFAULT '0' COMMENT '累计获得情绪值',
  `completed_count` int NOT NULL DEFAULT '0' COMMENT '累计完成委托数',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='红线仙情绪值';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `star_user_emotion`
--

LOCK TABLES `star_user_emotion` WRITE;
/*!40000 ALTER TABLE `star_user_emotion` DISABLE KEYS */;
/*!40000 ALTER TABLE `star_user_emotion` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-06 16:03:24
