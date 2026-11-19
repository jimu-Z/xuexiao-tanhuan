<template>
  <div class="talk-preview">
    <table class="preview-table">
      <tr><th>谈话类型</th><td>{{ record.talkType === 'group' ? '集体谈话' : '个别谈话' }}</td><th>谈话人</th><td>{{ record.talkPerson || '—' }}</td></tr>
      <tr><th>谈话时间</th><td>{{ record.talkTime || '—' }}</td><th>谈话地点</th><td>{{ record.talkLocation || '—' }}</td></tr>
      <tr><th>标签</th><td colspan="3">{{ tags.length ? tags.join('、') : '—' }}</td></tr>
      <tr><th>谈话内容</th><td colspan="3" class="preserve">{{ plainContent || '—' }}</td></tr>
      <template v-for="student in students">
        <tr :key="student.studentId + '-identity'"><th>学生</th><td colspan="3">{{ student.studentName || '—' }}（{{ student.studentCode || '—' }}） {{ details[student.studentId] && details[student.studentId].student.deptName || student.deptName || '' }}</td></tr>
        <tr :key="student.studentId + '-info'" v-if="details[student.studentId]"><th>学生信息</th><td colspan="3" class="preserve">性别：{{ $talkLabel('gender', details[student.studentId].student.gender) }}　民族：{{ details[student.studentId].student.nation || '—' }}　联系电话：{{ details[student.studentId].student.phone || '—' }}
政治面貌：{{ details[student.studentId].labels.politicalStatus || '—' }}　宿舍：{{ details[student.studentId].student.dormBuilding || '' }} {{ details[student.studentId].student.dormRoom || '' }}
学籍状态：{{ details[student.studentId].labels.enrollmentStatus || '—' }}　心理健康：{{ details[student.studentId].labels.mentalHealthStatus || '—' }}　贫困等级：{{ details[student.studentId].labels.povertyLevel || '—' }}
备注：{{ details[student.studentId].student.remark || '—' }}</td></tr>
        <tr :key="student.studentId + '-feedback'"><th>学生反馈</th><td colspan="3" class="preserve">{{ student.studentFeedback || student._feedback || '—' }}
跟进计划：{{ student.followupPlan || student._plan || '—' }}
跟进状态：{{ $talkLabel('followupStatus', student.followupStatus || 'pending') }}</td></tr>
        <tr :key="student.studentId + '-history'" v-if="details[student.studentId]"><th>历次谈话</th><td colspan="3"><div v-for="(item, index) in details[student.studentId].history" :key="index">{{ index + 1 }}. {{ item.talkTime || '未知' }}　{{ item.talkType === 'group' ? '集体' : '个别' }} | {{ item.talkPerson || '—' }}</div><span v-if="!details[student.studentId].history.length">无</span></td></tr>
      </template>
      <tr v-if="loading || failed"><th>资料</th><td colspan="3">{{ loading ? '正在加载学生资料与历次谈话…' : '部分学生资料无法加载，请检查权限或重试。' }} <el-button v-if="failed" size="mini" @click="loadDetails">重试</el-button></td></tr>
      <tr v-if="files.length"><th>附件</th><td colspan="3">{{ files.map(f => f.name || f.fileName).join('、') }}</td></tr>
    </table>
  </div>
</template>
<script>
import { getPreviewStudent, getPreviewText } from '@/api/talk/talkStudentRecord'
export default {
  name: 'TalkRecordPreview',
  props: { record: { type: Object, required: true }, students: { type: Array, default: () => [] }, tags: { type: Array, default: () => [] }, files: { type: Array, default: () => [] } },
  data() { return { details: {}, plainContent: '', loading: true, failed: false } },
  created() { this.loadDetails() },
  methods: {
    async loadDetails() {
      this.loading = true; this.failed = false; this.$emit('state-change', false)
      const details = {}; let index = 0
      const worker = async () => {
        while (index < this.students.length) {
          const student = this.students[index++]
          const result = await getPreviewStudent(student.studentId)
          details[student.studentId] = result.data
        }
      }
      try {
        const textPromise = getPreviewText(this.record.talkContent || '').then(result => { this.plainContent = result.data || ''; return true }, () => false)
        const outcomes = await Promise.all([textPromise, ...Array.from({ length: Math.min(5, this.students.length) }, () => worker().then(() => true, () => false))])
        if (outcomes.some(ok => !ok)) throw new Error('preview details unavailable')
        this.details = details; this.$emit('state-change', true)
      } catch (e) { this.failed = true; this.$emit('state-change', false) }
      finally { this.loading = false }
    }
  }
}
</script>
<style scoped>
.preview-table { width: 100%; border-collapse: collapse; table-layout: fixed; }
.preview-table th, .preview-table td { border: 1px solid #d8dce5; padding: 10px; overflow-wrap: anywhere; }
.preview-table th { width: 110px; background: #f5f7fa; text-align: left; }
.preserve { white-space: pre-wrap; }
</style>
