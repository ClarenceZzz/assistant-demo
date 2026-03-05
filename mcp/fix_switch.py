import os

base_dir = r"d:\code\Code\assistant-demo\mcp"

old_switch = """        return switch (city) {
            case "北京" -> "北京: 晴, 25°C";
            case "上海" -> "上海: 多云, 22°C";
            case "深圳" -> "深圳: 小雨, 28°C";
            default -> city + ": 下雪, -20°C";
        };"""

new_switch = """        switch (city) {
            case "北京": return "北京: 晴, 25°C";
            case "上海": return "上海: 多云, 22°C";
            case "深圳": return "深圳: 小雨, 28°C";
            default: return city + ": 下雪, -20°C";
        }"""

for root, dirs, files in os.walk(base_dir):
    if "WeatherService.java" in files:
        filepath = os.path.join(root, "WeatherService.java")
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        if old_switch in content:
            content = content.replace(old_switch, new_switch)
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"Fixed {filepath}")
