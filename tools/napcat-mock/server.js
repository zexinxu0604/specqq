const express = require('express');
const bodyParser = require('body-parser');

const app = express();
app.use(bodyParser.json());

// 模拟群组列表
const mockGroups = [
  { group_id: 123456789, group_name: "测试群组1", member_count: 50 },
  { group_id: 987654321, group_name: "测试群组2", member_count: 100 },
  { group_id: 111222333, group_name: "测试群组3", member_count: 200 }
];

// 获取群组列表
app.post('/get_group_list', (req, res) => {
  console.log('[NapCat Mock] Received get_group_list request');
  res.json({
    status: 'ok',
    retcode: 0,
    data: { groups: mockGroups }
  });
});

// 获取单个群组信息
app.post('/get_group_info', (req, res) => {
  const { group_id } = req.body;
  console.log(`[NapCat Mock] Received get_group_info request for group ${group_id}`);

  const group = mockGroups.find(g => g.group_id == group_id);
  if (group) {
    res.json({
      status: 'ok',
      retcode: 0,
      data: group
    });
  } else {
    res.json({
      status: 'failed',
      retcode: 1404,
      message: 'Group not found',
      data: null
    });
  }
});

// 模拟群组加入事件 (WebSocket 事件模拟)
app.post('/simulate_group_increase', (req, res) => {
  const { group_id, group_name, member_count } = req.body;
  console.log(`[NapCat Mock] Simulating group_increase event for group ${group_id}`);

  // 添加到模拟群组列表
  mockGroups.push({ group_id, group_name, member_count });

  res.json({
    status: 'ok',
    message: 'Group increase event simulated'
  });
});

const PORT = 3100;
app.listen(PORT, () => {
  console.log(`[NapCat Mock Server] Running on http://localhost:${PORT}`);
  console.log('Available endpoints:');
  console.log('  POST /get_group_list');
  console.log('  POST /get_group_info');
  console.log('  POST /simulate_group_increase');
});
