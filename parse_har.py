import json
import re

with open('jwgl.suse.edu.cn-学业情况.har', 'r', encoding='utf-8') as f:
    har = json.load(f)

for entry in har['log']['entries']:
    url = entry['request']['url']
    if 'xsxyqk_cxXsxyqkIndex' in url:
        response_text = entry['response']['content'].get('text', '')
        if response_text:
            # 提取所有类别和对应的 xfyqjd_id
            pattern = r"xfyqjd_id='([A-F0-9]+)'\s+data-content='([^']+)'"
            categories = re.findall(pattern, response_text)
            unique_categories = list(set(categories))
            print('找到的课程类别:')
            for cat_id, cat_name in unique_categories:
                print(f'  ID: {cat_id}')
                print(f'  名称: {cat_name}')
                print('---')
