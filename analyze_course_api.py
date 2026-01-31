import json
import re

with open('jwgl.suse.edu.cn.har', 'r', encoding='utf-8') as f:
    har = json.load(f)

print("=" * 60)
print("1. 查找教学计划列表 API")
print("=" * 60)

for entry in har['log']['entries']:
    url = entry['request']['url']
    if 'jxzxjhxx' in url or 'jxzxjhglList' in url:
        print(f"URL: {url}")
        method = entry['request']['method']
        print(f"Method: {method}")
        if entry['request'].get('postData'):
            print(f"PostData: {entry['request']['postData'].get('text', '')}")
        response = entry.get('response', {})
        content = response.get('content', {})
        text = content.get('text', '')
        if text and len(text) < 3000:
            print(f"Response: {text[:2000]}")
        print('---')

print("\n" + "=" * 60)
print("2. 查找主页面获取学院/专业/年级选项")
print("=" * 60)

for entry in har['log']['entries']:
    url = entry['request']['url']
    if 'jxzxjhkcxx_cxJxzxjhkcxxIndex' in url and 'doType' not in url:
        print(f"URL: {url}")
        response = entry.get('response', {})
        content = response.get('content', {})
        text = content.get('text', '')
        if text:
            # 提取学院选项
            jg_options = re.findall(r'id="jg_id"[^>]*>([^<]+(?:<option[^<]+</option>)+)', text, re.DOTALL)
            print(f"jg_id select content found: {len(jg_options)}")
            
            # 提取年级选项
            nj_options = re.findall(r'id="njdm_id"[^>]*>([^<]+(?:<option[^<]+</option>)+)', text, re.DOTALL)
            print(f"njdm_id select content found: {len(nj_options)}")
            
            # 直接找所有select块
            all_selects = re.findall(r'<select[^>]+id="([^"]+)"[^>]*>', text)
            print(f"All select IDs: {all_selects}")
            
            # 找年级选项
            nj_match = re.findall(r'name="njdm_id".*?</select>', text, re.DOTALL)
            if nj_match:
                nj_opts = re.findall(r'<option value="([^"]*)"[^>]*>([^<]*)</option>', nj_match[0])
                print(f"年级选项: {nj_opts}")
        print('---')

print("\n" + "=" * 60)
print("3. 查找jxzxjhgl相关请求")  
print("=" * 60)

for entry in har['log']['entries']:
    url = entry['request']['url']
    if 'jxzxjhgl' in url and 'js' not in url:
        print(f"URL: {url}")
        method = entry['request']['method']
        print(f"Method: {method}")
        if entry['request'].get('postData'):
            print(f"PostData: {entry['request']['postData'].get('text', '')[:300]}")
        print('---')

