<template>
  <div class="upload-container">
    <h2>🚴 FIT 파일 업로드</h2>

    <input type="file" @change="onFileChange" />
    <button @click="uploadFile" :disabled="!selectedFile">업로드</button>

    <div v-if="uploadStatus" class="status">
      {{ uploadStatus }}
    </div>

    <div v-if="fileList.length > 0" class="file-list">
      <h3>📄 서버에 저장된 파일 목록</h3>
      <ul>
        <li v-for="(file, index) in fileList" :key="index">
          {{ file }}
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';

const selectedFile = ref(null);
const uploadStatus = ref('');
const fileList = ref([]);  // ⭐ 파일 리스트 저장하는 곳

const onFileChange = (event) => {
  selectedFile.value = event.target.files[0];
};

const uploadFile = async () => {
  if (!selectedFile.value) return;

  const formData = new FormData();
  formData.append('file', selectedFile.value);

  try {
    const response = await axios.post('/api/fit/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });

    uploadStatus.value = '✅ 업로드 성공: ' + response.data;

    // ✅ 업로드 성공 후 서버 파일 리스트 다시 불러오기
    await fetchFileList();
  } catch (error) {
    console.error('❌ 업로드 실패', error);
    uploadStatus.value = '❌ 업로드 실패';
  }
};

// ✅ 서버에 저장된 파일 리스트 불러오기
const fetchFileList = async () => {
  try {
    const res = await axios.get('/api/fit/files');
    fileList.value = res.data.map(item => item.filename); // ActivityCoreVO에서 filename만 가져오기
  } catch (error) {
    console.error('❌ 파일 리스트 불러오기 실패', error);
  }
};

// 페이지 로드될 때도 파일 리스트 보여주기
onMounted(() => {
  fetchFileList();
});
</script>

<style scoped>
.upload-container {
  padding: 2rem;
  text-align: center;
}

input[type="file"] {
  margin-bottom: 1rem;
}

button {
  padding: 0.5rem 1.5rem;
  font-size: 1rem;
  cursor: pointer;
}

.status {
  margin-top: 1rem;
  font-weight: bold;
}

.file-list {
  margin-top: 2rem;
  text-align: left;
  max-width: 500px;
  margin-left: auto;
  margin-right: auto;
}

.file-list ul {
  list-style: none;
  padding: 0;
}

.file-list li {
  padding: 5px 0;
  border-bottom: 1px solid #eee;
}
</style>
