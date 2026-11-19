<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" class="talk-filter-form" label-position="top" v-show="showSearch">
      <el-form-item label="学号" prop="studentCode">
        <el-input
          v-model="queryParams.studentCode"
          placeholder="请输入学号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="姓名" prop="studentName">
        <el-input
          v-model="queryParams.studentName"
          placeholder="请输入姓名"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="班级" prop="deptId">
        <el-cascader v-model="queryParams.deptId" :options="deptTree"
          :props="{ checkStrictly:true, label:'label', value:'id' }"
          placeholder="请选择班级" clearable style="width:240px" @change="handleQuery" />
      </el-form-item>
      <el-form-item label="政治面貌" prop="politicalStatus">
        <el-select v-model="queryParams.politicalStatus" clearable placeholder="请选择政治面貌" @change="handleQuery">
          <el-option v-for="item in politicalStatusOptions" :key="item.dictValue" :label="item.dictLabel" :value="item.dictValue" />
        </el-select>
      </el-form-item>
      <el-form-item label="民族" prop="nation">
        <el-select v-model="queryParams.nation" filterable clearable placeholder="请选择民族" @change="handleQuery">
          <el-option v-for="item in nationOptions" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="学籍状态" prop="enrollmentStatus">
        <el-select v-model="queryParams.enrollmentStatus" clearable placeholder="请选择学籍状态" @change="handleQuery">
          <el-option v-for="item in enrollmentStatusOptions" :key="item.dictValue" :label="item.dictLabel" :value="item.dictValue" />
        </el-select>
      </el-form-item>
      <el-form-item label="心理健康状态" prop="mentalHealthStatus">
        <el-select v-model="queryParams.mentalHealthStatus" clearable placeholder="请选择心理健康状态" @change="handleQuery">
          <el-option v-for="item in mentalHealthStatusOptions" :key="item.dictValue" :label="item.dictLabel" :value="item.dictValue" />
        </el-select>
      </el-form-item>
      <el-form-item label="贫困等级认定" prop="povertyLevel">
        <el-select v-model="queryParams.povertyLevel" clearable placeholder="请选择贫困等级" @change="handleQuery">
          <el-option v-for="item in povertyLevelOptions" :key="item.dictValue" :label="item.dictLabel" :value="item.dictValue" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['talk:student:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['talk:student:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['talk:student:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['talk:student:export']"
        >导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          icon="el-icon-upload2"
          size="mini"
          @click="handleImportOpen"
          v-hasPermi="['talk:student:import']"
        >导入</el-button>
      </el-col>
      <el-col :span="1.5"><el-button type="info" plain icon="el-icon-picture" size="mini" v-hasPermi="['talk:student:import']" @click="photoImportOpen = true">照片导入</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="talkList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column label="学号" align="center" prop="studentCode" />
      <el-table-column label="姓名" align="center" prop="studentName"><template slot-scope="scope"><el-button type="text" @click="showStudentPhoto(scope.row)">{{ scope.row.studentName }}</el-button></template></el-table-column>
      <el-table-column label="班级" align="center" prop="deptName" :show-overflow-tooltip="true" />
      <el-table-column label="性别" align="center" prop="gender">
        <template slot-scope="scope">
          <span>{{ $talkLabel('gender', scope.row.gender, genderOptions) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="政治面貌" align="center" prop="politicalStatus"><template slot-scope="scope">{{ $talkLabel('politicalStatus', scope.row.politicalStatus, politicalStatusOptions) }}</template></el-table-column>
      <el-table-column label="民族" align="center" prop="nation" />
      <el-table-column label="本人联系电话" align="center" prop="phone">
        <template slot-scope="scope">{{ maskPhone(scope.row.phone) }}</template>
      </el-table-column>
      <el-table-column label="身份证号" align="center" prop="idCard">
        <template slot-scope="scope">{{ maskIdCard(scope.row.idCard) }}</template>
      </el-table-column>
      <el-table-column label="家庭住址" align="center" prop="address" />
      <el-table-column label="父亲姓名" align="center" prop="fatherName" />
      <el-table-column label="父亲电话" align="center" prop="fatherPhone" />
      <el-table-column label="母亲姓名" align="center" prop="motherName" />
      <el-table-column label="母亲电话" align="center" prop="motherPhone" />
      <el-table-column label="班长" align="center" prop="classMonitor" />
      <el-table-column label="舍长" align="center" prop="dormLeader" />
      <el-table-column label="宿舍楼" align="center" prop="dormBuilding" />
      <el-table-column label="宿舍号" align="center" prop="dormRoom" />
      <el-table-column label="学籍状态" align="center" prop="enrollmentStatus"><template slot-scope="scope">{{ $talkLabel('enrollmentStatus', scope.row.enrollmentStatus, enrollmentStatusOptions) }}</template></el-table-column>
      <el-table-column label="心理健康状态" align="center" prop="mentalHealthStatus"><template slot-scope="scope">{{ $talkLabel('mentalHealthStatus', scope.row.mentalHealthStatus, mentalHealthStatusOptions) }}</template></el-table-column>
      <el-table-column label="贫困等级认定" align="center" prop="povertyLevel"><template slot-scope="scope">{{ $talkLabel('povertyLevel', scope.row.povertyLevel, povertyLevelOptions) }}</template></el-table-column>
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-document"
            @click="handleDetail(scope.row)"
          >详情</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['talk:student:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['talk:student:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改学生信息管理对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="700px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="学号" prop="studentCode">
              <el-input v-model="form.studentCode" placeholder="请输入学号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="studentName">
              <el-input v-model="form.studentName" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="班级" prop="deptId">
              <el-cascader v-model="form.deptId" :options="deptTree"
                :props="{ checkStrictly:false, label:'label', value:'id' }"
                placeholder="请选择班级" clearable style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="辅导员">
              <el-input v-model="counselorDisplay" placeholder="选择班级后自动显示" readonly />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="书记">
              <el-input v-model="secretaryDisplay" placeholder="选择班级后自动显示" readonly />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-select v-model="form.gender" placeholder="请选择性别" style="width:100%">
                <el-option v-for="dict in genderOptions" :key="dict.dictValue" :label="dict.dictLabel" :value="dict.dictValue" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="政治面貌" prop="politicalStatus">
              <el-select v-model="form.politicalStatus" placeholder="请选择政治面貌" style="width:100%">
                <el-option v-for="dict in politicalStatusOptions" :key="dict.dictValue" :label="dict.dictLabel" :value="dict.dictValue" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="民族" prop="nation">
              <el-input v-model="form.nation" placeholder="请输入民族" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学籍状态" prop="enrollmentStatus">
              <el-select v-model="form.enrollmentStatus" placeholder="请选择学籍状态" style="width:100%">
                <el-option v-for="dict in enrollmentStatusOptions" :key="dict.dictValue" :label="dict.dictLabel" :value="dict.dictValue" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="心理健康" prop="mentalHealthStatus">
              <el-select v-model="form.mentalHealthStatus" placeholder="请选择心理健康状态" style="width:100%">
                <el-option v-for="dict in mentalHealthStatusOptions" :key="dict.dictValue" :label="dict.dictLabel" :value="dict.dictValue" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="贫困等级" prop="povertyLevel">
              <el-select v-model="form.povertyLevel" placeholder="请选择贫困等级" style="width:100%">
                <el-option v-for="dict in povertyLevelOptions" :key="dict.dictValue" :label="dict.dictLabel" :value="dict.dictValue" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="本人电话" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入本人联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证号" prop="idCard">
              <el-input v-model="form.idCard" placeholder="请输入身份证号" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="家庭住址" prop="address">
              <el-input v-model="form.address" placeholder="请输入家庭住址" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="父亲姓名" prop="fatherName">
              <el-input v-model="form.fatherName" placeholder="请输入父亲姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="父亲电话" prop="fatherPhone">
              <el-input v-model="form.fatherPhone" placeholder="请输入父亲电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="母亲姓名" prop="motherName">
              <el-input v-model="form.motherName" placeholder="请输入母亲姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="母亲电话" prop="motherPhone">
              <el-input v-model="form.motherPhone" placeholder="请输入母亲电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="班长" prop="classMonitor">
              <el-input v-model="form.classMonitor" placeholder="请输入班长" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="舍长" prop="dormLeader">
              <el-input v-model="form.dormLeader" placeholder="请输入舍长" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="宿舍楼" prop="dormBuilding">
              <el-input v-model="form.dormBuilding" placeholder="请输入宿舍楼" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="宿舍号" prop="dormRoom">
              <el-input v-model="form.dormRoom" placeholder="请输入宿舍号" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog title="学生照片导入" :visible.sync="photoImportOpen" width="850px" append-to-body :show-close="!photoLoading" :close-on-click-modal="!photoLoading" :close-on-press-escape="!photoLoading">
      <p>按“班级_学号_姓名.jpg”命名，按班放入文件夹后打包为 ZIP。每张图片不超过 3MB，压缩包不超过 50MB。</p>
      <el-upload action="" :auto-upload="false" :limit="1" accept=".zip" :disabled="photoLoading" :on-change="photoFileChange" :on-remove="photoFileRemove">
        <el-button :disabled="photoLoading" size="small" type="primary">选择 ZIP</el-button>
      </el-upload>
      <el-button size="small" :disabled="!photoZip || photoLoading" :loading="photoLoading" @click="previewPhotos">预检查</el-button>
      <el-checkbox v-model="photoReplace" :disabled="photoLoading">替换已有照片</el-checkbox>
      <p v-if="photoRows.length">{{ photoSummary }}</p>
      <el-button v-if="photoRows.some(row => row.status === 'error')" size="mini" @click="downloadPhotoErrors">下载失败清单</el-button>
      <el-table v-if="photoRows.length" :data="photoRows" max-height="350" size="mini">
        <el-table-column prop="className" label="班级" /><el-table-column prop="studentCode" label="学号" /><el-table-column prop="studentName" label="姓名" />
        <el-table-column label="结果"><template slot-scope="scope">{{ photoStatus(scope.row.status) }} {{ scope.row.message }}</template></el-table-column>
      </el-table>
      <span slot="footer"><el-button :disabled="photoLoading" @click="photoImportOpen = false">关闭</el-button><el-button type="primary" :disabled="!photoZip || !photoRows.length || photoFinished" :loading="photoLoading" @click="executePhotos">确认导入</el-button></span>
    </el-dialog>
    <el-dialog :title="photoViewTitle" :visible.sync="photoViewOpen" width="400px" append-to-body @closed="clearPhotoUrl">
      <div v-loading="photoViewLoading" style="text-align:center; min-height:120px"><img v-if="photoUrl" :src="photoUrl" alt="学生照片" style="max-width:100%; max-height:400px" /><span v-else>{{ photoViewMessage }}</span></div>
    </el-dialog>

    <!-- 导入对话框 -->
    <el-dialog :title="importTitle" :visible.sync="importOpen" width="800px" append-to-body
      :show-close="!importLoading" :close-on-click-modal="!importLoading" :close-on-press-escape="!importLoading">
      <el-alert v-if="importLoading" title="导入处理中，请保持页面打开，不要重复提交。批量导入可能需要几分钟。"
        type="info" :closable="false" show-icon style="margin-bottom:12px" />
      <div v-if="importStep === 1">
        <el-upload
          ref="upload"
          action=""
          drag
          :auto-upload="false"
          :limit="1"
          accept=".xlsx,.xls"
          :on-change="handleFileChange"
          :on-remove="handleFileRemove"
          :file-list="fileList"
        >
          <i class="el-icon-upload"></i>
          <div class="el-upload__text">将Excel文件拖到此处，或<em>点击上传</em></div>
          <div class="el-upload__tip" slot="tip">
            支持 .xlsx / .xls 格式；使用下载模板，从第3行开始填写，系统自动识别标题行
            <div>教师列可填本学院教师姓名或工号，不限岗位；多人用顿号分隔。导入后增量绑定学生和班级，不覆盖已有管理人。</div>
            <div>新建学生账号以学号为用户名，分配学生角色，初始密码123456；学生端暂不开通，账号保持停用。覆盖导入不重置已有密码。</div>
            <el-link type="primary" :underline="false" style="font-size:12px;vertical-align:baseline;margin-left:8px" @click="handleDownloadTemplate">下载模板</el-link>
          </div>
        </el-upload>
        <div style="margin-top:12px">
          <span style="font-size:13px;color:#666;margin-right:8px">重复处理：</span>
          <el-radio-group v-model="importMode">
            <el-radio label="skip">跳过重复学号</el-radio>
            <el-radio label="update">覆盖已有数据</el-radio>
          </el-radio-group>
        </div>
        <div style="text-align: center; margin-top: 20px;">
          <el-button type="primary" :loading="importLoading" @click="handleImportPreview" :disabled="!uploadFile">开始预览</el-button>
        </div>
      </div>
      <div v-else-if="importStep === 2">
        <el-alert
          :title="'共 ' + importResult.totalRows + ' 行数据，正确 ' + (importResult.totalRows - importResult.errorCount - importResult.warnCount) + ' 行，警告 ' + importResult.warnCount + ' 行，错误 ' + importResult.errorCount + ' 行'"
          :type="importResult.errorCount > 0 ? 'warning' : 'success'"
          :closable="false"
          show-icon
        />
        <div style="overflow-x: auto; margin-top: 10px;">
        <el-table :data="importResult.previewRows" max-height="400" border size="mini">
          <el-table-column label="行号" prop="rowNum" width="60" fixed="left" />
          <el-table-column label="学号" prop="data.student_code" width="130" />
          <el-table-column label="姓名" prop="data.student_name" width="80" />
          <el-table-column label="学院" prop="data.college" width="150" />
          <el-table-column label="年级" prop="data.grade" width="80" />
          <el-table-column label="班级" prop="data.class" width="120" />
          <el-table-column label="电话" prop="data.phone" width="120" />
          <el-table-column label="身份证号" prop="data.id_card" width="180" />
          <el-table-column label="家庭住址" prop="data.address" min-width="150" show-overflow-tooltip />
          <el-table-column label="状态" width="70" fixed="right">
            <template slot-scope="scope">
              <el-tag v-if="scope.row.status === 'ok'" type="success">正常</el-tag>
              <el-tag v-else-if="scope.row.status === 'warn'" type="warning">警告</el-tag>
              <el-tag v-else type="danger">错误</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="提示" min-width="120" show-overflow-tooltip>
            <template slot-scope="scope">{{ importMode === 'update' && scope.row.message === '学号已存在，将跳过此条记录' ? '学号已存在，将覆盖已有数据' : scope.row.message }}</template>
          </el-table-column>
        </el-table>
        </div>
        <div slot="footer" class="dialog-footer" style="text-align: center; margin-top: 15px;">
          <el-button :disabled="importLoading" @click="importStep = 1">返回</el-button>
          <el-button type="primary" :loading="importLoading" @click="handleImportExecute" :disabled="importResult.errorCount >= importResult.totalRows">确认导入</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getStudentPhoto, previewStudentPhotos, importStudentPhotos, getNations, buildStudentQuery, listTalk, getTalk, delTalk, addTalk, updateTalk, importPreview, importExecute, getDeptTree } from "@/api/talk/talkStudent"
import { getDicts } from "@/api/system/dict/data"
import request from "@/utils/request"
import { sortGradeNodes } from "@/utils/queryOptions"

export default {
  name: "Talk",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 学生信息管理表格数据
      talkList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        studentCode: null,
        studentName: null,
        deptId: null,
        gender: null,
        politicalStatus: null,
        nation: null,
        phone: null,
        idCard: null,
        address: null,
        fatherName: null,
        fatherPhone: null,
        motherName: null,
        motherPhone: null,
        classMonitor: null,
        dormLeader: null,
        dormBuilding: null,
        dormRoom: null,
        enrollmentStatus: null,
        mentalHealthStatus: null,
        povertyLevel: null,
        dateRange: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        studentCode: [
          { required: true, message: "学号不能为空", trigger: "blur" }
        ],
        studentName: [
          { required: true, message: "姓名不能为空", trigger: "blur" }
        ],
        deptId: [
          { required: true, message: "请选择班级", trigger: "change" }
        ],
      },
      // 导入相关
      importOpen: false,
      importTitle: "导入学生数据",
      importStep: 1,
      importLoading: false,
      uploadFile: null,
      fileList: [],
      importResult: { totalRows: 0, errorCount: 0, warnCount: 0, previewRows: [] },
      importMode: 'skip',
      deptTree: [],
      nationOptions: [],
      photoImportOpen: false, photoLoading: false, photoZip: null, photoRows: [], photoReplace: false, photoFinished: false,
      photoViewOpen: false, photoViewLoading: false, photoViewTitle: '', photoViewMessage: '', photoUrl: '', photoRequestSeq: 0,
      // 辅导员/书记展示
      counselorDisplay: '',
      secretaryDisplay: '',
      // 字典选项
      genderOptions: [{ dictValue: '0', dictLabel: '男' }, { dictValue: '1', dictLabel: '女' }],
      politicalStatusOptions: [],
      enrollmentStatusOptions: [],
      mentalHealthStatusOptions: [],
      povertyLevelOptions: []
    }
  },
  computed: {
    photoSummary() {
      const count = status => this.photoRows.filter(row => row.status === status).length
      return '共 ' + this.photoRows.length + ' 条，可导入 ' + count('ready') + '，已有 ' + count('existing') + '，已保存 ' + count('saved') + '，跳过 ' + count('skipped') + '，失败 ' + count('error')
    }
  },
  created() {
    this.getList()
    this.loadDeptTree()
    this.loadDicts()
    this.loadNations()
  },
  beforeDestroy() { this.clearPhotoUrl() },
  methods: {
    downloadPhotoErrors() {
      const csv = value => '"' + ('\t' + String(value || '')).replace(/"/g, '""') + '"'
      const rows = [['班级', '学号', '姓名/文件名', '原因'], ...this.photoRows.filter(row => row.status === 'error').map(row => [row.className, row.studentCode, row.studentName, row.message])]
      const url = URL.createObjectURL(new Blob(['\ufeff' + rows.map(row => row.map(csv).join(',')).join('\r\n')], { type: 'text/csv;charset=utf-8' }))
      const link = document.createElement('a'); link.href = url; link.download = '照片导入失败清单.csv'; link.click(); URL.revokeObjectURL(url)
    },
    photoFileChange(file) { this.photoZip = file.raw; this.photoRows = []; this.photoFinished = false },
    photoFileRemove() { this.photoZip = null; this.photoRows = []; this.photoFinished = false },
    photoStatus(value) { return ({ ready: '可导入', existing: '已有照片', error: '失败', skipped: '已跳过', saved: '已保存' })[value] || value },
    photoForm() { const data = new FormData(); data.append('file', this.photoZip); return data },
    previewPhotos() {
      if (!this.photoZip || this.photoLoading) return
      this.photoLoading = true
      previewStudentPhotos(this.photoForm()).then(res => { this.photoRows = res.data || []; this.photoFinished = false }).catch(() => { this.$modal.msgError('照片预检查失败，请检查压缩包格式') }).finally(() => { this.photoLoading = false })
    },
    executePhotos() {
      if (!this.photoZip || !this.photoRows.length || this.photoLoading || this.photoFinished) return
      const existing = this.photoRows.filter(row => row.status === 'existing').length
      const action = this.photoReplace ? this.$modal.confirm('预检查时有 ' + existing + ' 名学生已有照片。确认替换已有照片？导入时数量可能变化。') : Promise.resolve()
      action.then(() => {
        if (this.photoLoading) return
        this.photoLoading = true
        return importStudentPhotos(this.photoForm(), this.photoReplace).then(res => {
          this.photoRows = res.data || []; this.photoFinished = true
          this.$modal.msgSuccess('处理完成，请查看每行结果')
        }).catch(() => { this.photoFinished = true; this.$modal.msgError('请求未正常返回，部分照片可能已保存，请先重新预检查，不要重复提交') }).finally(() => { this.photoLoading = false })
      }).catch(() => {})
    },
    clearPhotoUrl() { if (this.photoUrl) URL.revokeObjectURL(this.photoUrl); this.photoUrl = ''; this.photoRequestSeq++ },
    showStudentPhoto(row) {
      this.clearPhotoUrl(); const seq = this.photoRequestSeq
      this.photoViewTitle = row.studentName + ' 的照片'; this.photoViewMessage = ''; this.photoViewOpen = true; this.photoViewLoading = true
      getStudentPhoto(row.studentId).then(blob => {
        if (seq !== this.photoRequestSeq) return
        if (!blob.type.startsWith('image/')) throw new Error('invalid photo response')
        this.photoUrl = URL.createObjectURL(blob)
      }).catch(error => { if (seq === this.photoRequestSeq) this.photoViewMessage = error && error.response && error.response.status === 404 ? '暂无照片' : '照片读取失败，请稍后重试' }).finally(() => { if (seq === this.photoRequestSeq) this.photoViewLoading = false })
    },
    loadNations() {
      getNations().then(res => { this.nationOptions = res.data || [] }).catch(() => { this.$modal.msgError("民族选项加载失败，请刷新后重试") })
    },
    loadDicts() {
      getDicts("political_status").then(res => { this.politicalStatusOptions = res.data || [] })
      getDicts("enrollment_status").then(res => { this.enrollmentStatusOptions = res.data || [] })
      getDicts("mental_health_status").then(res => { this.mentalHealthStatusOptions = res.data || [] })
      getDicts("poverty_level").then(res => { this.povertyLevelOptions = res.data || [] })
    },
    loadDeptTree() {
      getDeptTree().then(res => {
        this.deptTree = this.mapDeptTree(sortGradeNodes(res.data || []))
      }).catch(() => { this.$modal.msgError('操作失败') })
    },
    mapDeptTree(nodes) {
      return nodes.map(n => {
        const node = { id: n.deptId, label: n.deptName, deptType: n.deptType, parentId: n.parentId }
        if (n.children && n.children.length) node.children = this.mapDeptTree(n.children)
        return node
      })
    },
    /** 在部门树中查找从根到目标节点的路径 */
    findDeptPath(targetId, tree, path = []) {
      const t = String(targetId)
      for (const node of tree) {
        if (String(node.id) === t) {
          return [...path, node.id]
        }
        if (node.children && node.children.length > 0) {
          const found = this.findDeptPath(targetId, node.children, [...path, node.id])
          if (found) return found
        }
      }
      return null
    },
    /** 查询学生信息管理列表 */
    maskPhone(phone) {
      if (!phone || phone.length < 7) return phone || '-'
      return phone.substring(0, 3) + '****' + phone.substring(phone.length - 4)
    },
    maskIdCard(idCard) {
      if (!idCard || idCard.length < 8) return idCard || '-'
      return idCard.substring(0, 3) + '***********' + idCard.substring(idCard.length - 4)
    },
    getList() {
      this.loading = true
      listTalk(this.queryParams).then(response => {
        this.talkList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    // 取消按钮
    cancel() {
      this.open = false
      this.reset()
    },
    // 表单重置
    reset() {
      this.form = {
        studentId: null,
        studentCode: null,
        studentName: null,
        deptId: null,
        gender: null,
        politicalStatus: null,
        nation: null,
        phone: null,
        idCard: null,
        address: null,
        fatherName: null,
        fatherPhone: null,
        motherName: null,
        motherPhone: null,
        classMonitor: null,
        dormLeader: null,
        dormBuilding: null,
        dormRoom: null,
        enrollmentStatus: null,
        mentalHealthStatus: null,
        povertyLevel: null,
        remark: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null
      }
      this.counselorDisplay = ''
      this.secretaryDisplay = ''
      this.resetForm("form")
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.studentId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加学生信息管理"
    },
    /** 查看详情 */
    handleDetail(row) {
      this.$router.push({ path: '/talk/student-detail/' + row.studentId })
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const studentId = row && row.studentId != null ? row.studentId : (this.ids && this.ids.length > 0 ? this.ids[0] : null)
      getTalk(studentId).then(response => {
        this.form = response.data || {}
        // 将 deptId 转换为级联路径数组以便回显
        if (this.form.deptId && this.deptTree) {
          const path = this.findDeptPath(this.form.deptId, this.deptTree)
          if (path) {
            this.form.deptId = path
          }
        }
        // 加载辅导员/书记信息
        const leafDeptId = Array.isArray(this.form.deptId) ? this.form.deptId[this.form.deptId.length - 1] : this.form.deptId
        this.loadManagers(leafDeptId)
        this.open = true
        this.title = "修改学生信息管理"
      }).catch(() => { this.$modal.msgError('获取学生详情失败') })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          const submitData = { ...this.form }
          // 级联选择器返回的是数组路径，取最后一个叶子节点 id
          if (Array.isArray(submitData.deptId)) {
            submitData.deptId = submitData.deptId[submitData.deptId.length - 1]
          }
          if (submitData.studentId != null) {
            updateTalk(submitData).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            }).catch(() => { this.$modal.msgError('修改失败') })
          } else {
            addTalk(submitData).then(response => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            }).catch(() => { this.$modal.msgError('新增失败') })
          }
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const studentIds = row.studentId || this.ids
      this.$modal.confirm('是否确认删除学生信息管理编号为"' + studentIds + '"的数据项？').then(function() {
        return delTalk(studentIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(error => { if (error !== 'cancel' && error !== 'close') this.$modal.msgError('删除失败') })
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('ruoyi-system/talk/export', {
        ...buildStudentQuery(this.queryParams)
      }, `talk_${new Date().getTime()}.xlsx`)
    },
    /** 打开导入对话框 */
    handleImportOpen() {
      if (this.importLoading) return
      this.importStep = 1
      this.importOpen = true
      this.uploadFile = null
      this.fileList = []
    },
    /** 下载导入模板 */
    handleDownloadTemplate() {
      this.download('ruoyi-system/talk/importTemplate', {}, `德州学院学生教育管理台账模板_${new Date().getTime()}.xlsx`)
    },
    /** 文件选择变化 */
    handleFileChange(file) {
      const raw = file && file.raw
      if (!raw || !/\.(xlsx|xls)$/i.test(raw.name || file.name || '')) {
        this.uploadFile = null
        this.fileList = []
        this.$modal.msgError('仅支持 .xlsx 或 .xls 格式的文件')
        return
      }
      this.uploadFile = raw
      this.fileList = [file]
    },
    handleFileRemove() {
      this.uploadFile = null
      this.fileList = []
    },
    /** 导入预览 */
    handleImportPreview() {
      if (this.importLoading) return
      if (!this.uploadFile) {
        this.$modal.msgError("请先选择文件")
        return
      }
      this.importLoading = true
      const formData = new FormData()
      formData.append('file', this.uploadFile)
      importPreview(formData).then(response => {
        this.importResult = response.data
        this.importStep = 2
        this.importLoading = false
      }).catch(() => {
        this.importLoading = false
      })
    },
    /** 执行导入 */
    handleImportExecute() {
      if (this.importLoading) return
      this.importLoading = true
      const importData = this.importResult.previewRows.map(row => row.data)
      importExecute(importData, this.importMode).then(response => {
        const data = response.data
        const msg = '成功导入 ' + (data.successCount || 0) + ' 条，跳过 ' + (data.skipCount || 0) + ' 条，失败 ' + (data.errorCount || 0) + ' 条，新建学生账号 ' + (data.userCreatedCount || 0) + ' 个'
        const errors = data.errors || []
        if (errors.length > 0) {
          this.$alert(
            this.$createElement('div', { style: { maxHeight: '300px', overflowY: 'auto' } },
              errors.map(error => this.$createElement('div', {
                style: { margin: '4px 0', color: '#e6a23c' }
              }, [String(error)]))
            ),
            msg,
            { confirmButtonText: '知道了', type: data.successCount > 0 ? 'warning' : 'error' }
          )
        } else {
          this.$modal.msgSuccess(msg)
        }
        this.importOpen = false
        this.importLoading = false
        this.getList()
      }).catch(() => {
        this.importLoading = false
        this.$alert('导入请求未正常返回，后台可能仍在处理或已完成。请先刷新列表核对本批学生，不要重复提交；确认仍有缺失后再使用跳过重复模式补导。', '请先核对导入结果', {
          confirmButtonText: '知道了', type: 'warning'
        })
      })
    },
    /** 根据班级ID加载辅导员/书记 */
    loadManagers(deptId) {
      if (!deptId) {
        this.counselorDisplay = ''
        this.secretaryDisplay = ''
        return
      }
      request({ url: '/talk/teacher/managers/' + deptId, method: 'get' }).then(res => {
        const list = res.data || []
        const counselors = list.filter(t => t.position === '辅导员' || t.position === '班主任')
        const secretaries = list.filter(t => t.position === '书记' || t.position === '副书记')
        this.counselorDisplay = counselors.map(t => t.teacherName + (t.phone ? '(' + t.phone + ')' : '')).join('、') || '暂无'
        this.secretaryDisplay = secretaries.map(t => t.teacherName + (t.phone ? '(' + t.phone + ')' : '')).join('、') || '暂无'
      }).catch(() => {
        this.counselorDisplay = ''
        this.secretaryDisplay = ''
      })
    }
  },
  watch: {
    'form.deptId'(val) {
      const leafDeptId = Array.isArray(val) ? val[val.length - 1] : val
      this.loadManagers(leafDeptId)
    }
  }
}
</script>
