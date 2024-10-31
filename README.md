# SmartScheduleEye

## 规则

CT = DR = 体检 =（DR+体检）= 8.上班

日 = * = 16.下班

公 = 休 = 睡大觉

值 = 16.上到第二天8.

出 = 早上8.下班回家睡大觉



| 2024.9.23 | 2024.9.24 | 2024.9.25 | 2024.9.26 | 2024.9.27 | 2024.9.28 | 2024.9.29 |
| --------- | --------- | --------- | --------- | --------- | --------- | --------- |
| (值)      |           |           |           |           |           |           |
| 值夜班🌙   | 早班结束🌞 | 睡大觉喽😴 | 正常上班😊 | 没有午休😭 | 睡大觉喽😴 | 睡大觉喽😴 |






对这个 结果进行处理 可以参考python代码

class ScheduleType(Enum):
    # 正常工作时间 8:00-17:00
    NORMAL_WORK = "NORMAL_WORK"  # CT/DR/体检/DR+检

    # 短班时间 8:00-16:00
    SHORT_WORK = "SHORT_WORK"    # 带*号的班次

    # 休息
    REST = "REST"                # 公休/休

    # 值班 16:00-次日8:00
    NIGHT_SHIFT = "NIGHT_SHIFT"  # (值)

    # 早班结束 到8:00下班
    MORNING_END = "MORNING_END"  # 出

    @classmethod
    def from_schedule_text(cls, text: str) -> 'ScheduleType':
        """根据排班文本返回对应的班次类型"""
        if not text:
            return None

        text = text.upper()
        if any(x in text for x in ['*', '日']):
            return cls.SHORT_WORK
        elif any(x in text for x in ['CT', 'DR', '体检', '检']):
            return cls.NORMAL_WORK
        elif any(x in text for x in ['公', '休']):
            return cls.REST
        elif '值' in text:
            return cls.NIGHT_SHIFT
        elif '出' in text:
            return cls.MORNING_END
        return None

接下来对jsonObject的日期 进行处理 使用当前年份 读取里面的日期

然后 在分隔栏下方、显示一个表格
这个表格一定是7个类似于
