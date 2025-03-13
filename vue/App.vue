<template>
  <v-app>
    <!-- App Bar with Gradient and Shadow -->
    <v-app-bar app dark elevate-on-scroll color="primary" class="gradient-bar">
      <v-toolbar-title class="headline font-weight-bold">Social Assistance Hub</v-toolbar-title>
      <v-spacer></v-spacer>
      <v-btn v-if="isAuthenticated" color="white" outlined @click="logout" class="mr-2">
        <v-icon left>mdi-logout</v-icon> Logout
      </v-btn>
    </v-app-bar>

    <v-main class="background">
      <!-- Login Section -->
      <v-container v-if="!isAuthenticated" class="d-flex align-center justify-center" style="min-height: 80vh;">
        <v-row justify="center">
          <v-col cols="12" sm="6" md="4">
            <v-card class="pa-6 rounded-xl" elevation="10" color="white">
              <v-card-title class="text-h5 font-weight-bold primary--text text-center">Welcome Back</v-card-title>
              <v-card-text>
                <v-text-field v-model="loginForm.username" label="Username" prepend-inner-icon="mdi-account" outlined rounded dense required class="mb-4"></v-text-field>
                <v-text-field v-model="loginForm.password" label="Password" prepend-inner-icon="mdi-lock" type="password" outlined rounded dense required class="mb-4"></v-text-field>
                <v-btn block color="primary" large rounded @click="login" :loading="loading" class="gradient-btn">Login</v-btn>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>
      </v-container>

      <!-- Main Dashboard -->
      <v-container v-else class="py-8">
        <v-row>
          <v-col cols="12">
            <v-tabs v-model="activeTab" background-color="transparent" centered grow class="rounded-lg elevation-4" color="primary">
              <v-tab v-for="tab in tabs" :key="tab" class="text-capitalize font-weight-bold" :disabled="tab === 'Reports' && userRole !== 'ROLE_ADMIN'">{{ tab }}</v-tab>
            </v-tabs>
          </v-col>
        </v-row>

        <!-- Applicants Tab -->
        <v-tab-item>
          <v-card class="mt-6 pa-6 rounded-xl" elevation="6" color="grey lighten-4">
            <v-card-title class="text-h5 font-weight-bold primary--text">Applicants Management</v-card-title>
            <v-card-text>
              <v-row align="center">
                <v-col cols="12" md="4">
                  <v-text-field v-model="applicantFilters.name" label="Search by Name" prepend-inner-icon="mdi-magnify" outlined rounded dense clearable @input="fetchApplicants"></v-text-field>
                </v-col>
                <v-col cols="12" md="4">
                  <v-text-field v-model="applicantFilters.idNumber" label="Search by National ID" prepend-inner-icon="mdi-card-account-details" outlined rounded dense clearable @input="fetchApplicants"></v-text-field>
                </v-col>
                <v-col cols="12" md="4">
                  <v-menu v-model="dateMenu" :close-on-content-click="false" transition="scale-transition" offset-y max-width="290px" min-width="auto">
                    <template v-slot:activator="{ on, attrs }">
                      <v-text-field v-model="applicantFilters.dateApplied" label="Search by Date Applied" prepend-inner-icon="mdi-calendar" outlined rounded dense clearable readonly v-bind="attrs" v-on="on" @input="fetchApplicants"></v-text-field>
                    </template>
                    <v-date-picker v-model="applicantFilters.dateApplied" no-title @input="dateMenu = false; fetchApplicants()"></v-date-picker>
                  </v-menu>
                </v-col>
              </v-row>
              <v-data-table :headers="applicantHeaders" :items="applicants" :loading="loading" class="elevation-2 rounded-lg" hide-default-footer>
                <template v-slot:item.actions="{ item }">
                  <v-btn small color="primary" rounded @click="verifyApplicant(item.id)" v-if="userRole === 'ROLE_VERIFIER'" class="mr-2">Verify</v-btn>
                  <v-btn small color="error" rounded @click="deleteApplicant(item.id)" v-if="userRole === 'ROLE_ADMIN'">Delete</v-btn>
                </template>
              </v-data-table>
              <v-btn color="primary" rounded class="mt-4 gradient-btn" @click="fetchApplicants"><v-icon left>mdi-refresh</v-icon> Refresh</v-btn>
            </v-card-text>
          </v-card>

          <!-- Applicant Creation Form -->
          <v-dialog v-model="showApplicantForm" max-width="700px" persistent>
            <v-card class="pa-6 rounded-xl" color="white" elevation="10">
              <v-card-title class="text-h5 font-weight-bold success--text">New Applicant</v-card-title>
              <v-card-text>
                <v-form ref="applicantForm" @submit.prevent="createApplicant">
                  <v-row>
                    <v-col cols="12" sm="6">
                      <v-text-field v-model="newApplicant.firstName" label="First Name" prepend-inner-icon="mdi-account" outlined rounded dense required :rules="[v => !!v || 'Required']"></v-text-field>
                    </v-col>
                    <v-col cols="12" sm="6">
                      <v-text-field v-model="newApplicant.middleName" label="Middle Name" prepend-inner-icon="mdi-account" outlined rounded dense></v-text-field>
                    </v-col>
                    <v-col cols="12" sm="6">
                      <v-text-field v-model="newApplicant.lastName" label="Last Name" prepend-inner-icon="mdi-account" outlined rounded dense required :rules="[v => !!v || 'Required']"></v-text-field>
                    </v-col>
                    <v-col cols="12" sm="6">
                      <v-select v-model="newApplicant.sex" :items="sexOptions" item-text="value" item-value="id" label="Sex" prepend-inner-icon="mdi-gender-male-female" outlined rounded dense required :rules="[v => !!v || 'Required']"></v-select>
                    </v-col>
                    <v-col cols="12" sm="6">
                      <v-text-field v-model.number="newApplicant.age" label="Age" prepend-inner-icon="mdi-numeric" type="number" outlined rounded dense required :rules="[v => !!v || 'Required', v => v > 0 || 'Must be positive']"></v-text-field>
                    </v-col>
                    <v-col cols="12" sm="6">
                      <v-select v-model="newApplicant.maritalStatus" :items="maritalStatusOptions" item-text="value" item-value="id" label="Marital Status" prepend-inner-icon="mdi-ring" outlined rounded dense required :rules="[v => !!v || 'Required']"></v-select>
                    </v-col>
                    <v-col cols="12" sm="6">
                      <v-text-field v-model="newApplicant.idNumber" label="ID Number" prepend-inner-icon="mdi-card-account-details" outlined rounded dense required :rules="[v => !!v || 'Required']"></v-text-field>
                    </v-col>
                    <v-col cols="12" sm="6">
                      <v-select v-model="newApplicant.village" :items="villageOptions" item-text="name" item-value="id" label="Village" prepend-inner-icon="mdi-home-city" outlined rounded dense required :rules="[v => !!v || 'Required']"></v-select>
                    </v-col>
                    <v-col cols="12" sm="6">
                      <v-text-field v-model="newApplicant.postalAddress" label="Postal Address" prepend-inner-icon="mdi-mailbox" outlined rounded dense></v-text-field>
                    </v-col>
                    <v-col cols="12" sm="6">
                      <v-text-field v-model="newApplicant.physicalAddress" label="Physical Address" prepend-inner-icon="mdi-map-marker" outlined rounded dense></v-text-field>
                    </v-col>
                    <v-col cols="12">
                      <v-text-field v-model="newApplicant.telephone" label="Telephone" prepend-inner-icon="mdi-phone" outlined rounded dense></v-text-field>
                    </v-col>
                  </v-row>
                  <v-row justify="end">
                    <v-btn color="primary" rounded type="submit" :disabled="loading" class="mr-4 gradient-btn">Submit</v-btn>
                    <v-btn color="grey" rounded @click="showApplicantForm = false" outlined>Cancel</v-btn>
                  </v-row>
                </v-form>
              </v-card-text>
            </v-card>
          </v-dialog>
        </v-tab-item>

        <!-- Applications Tab -->
        <v-tab-item>
          <v-card class="mt-6 pa-6 rounded-xl" elevation="6" color="grey lighten-4">
            <v-card-title class="text-h5 font-weight-bold primary--text">Applications Dashboard</v-card-title>
            <v-card-text>
              <v-text-field v-model="applicationFilter.status" label="Filter by Status" prepend-inner-icon="mdi-filter" outlined rounded dense clearable @input="fetchApplications"></v-text-field>
              <v-data-table :headers="applicationHeaders" :items="applications" :loading="loading" class="elevation-2 rounded-lg" hide-default-footer>
                <template v-slot:item.actions="{ item }">
                  <v-btn small color="primary" rounded @click="approveApplication(item.id)" v-if="userRole === 'ROLE_APPROVER'" class="mr-2">Approve</v-btn>
                  <v-btn small color="error" rounded @click="deleteApplication(item.id)" v-if="userRole === 'ROLE_ADMIN'">Delete</v-btn>
                </template>
              </v-data-table>
              <v-row class="mt-4">
                <v-col>
                  <v-btn color="primary" rounded class="gradient-btn" @click="fetchApplications"><v-icon left>mdi-refresh</v-icon> Refresh</v-btn>
                </v-col>
                <v-col class="text-right">
                  <v-btn color="success" rounded class="gradient-btn" @click="exportApplications"><v-icon left>mdi-download</v-icon> Export</v-btn>
                </v-col>
              </v-row>
            </v-card-text>
          </v-card>
        </v-tab-item>

        <!-- Uploads Tab -->
        <v-tab-item>
          <v-card class="mt-6 pa-6 rounded-xl" elevation="6" color="grey lighten-4">
            <v-card-title class="text-h5 font-weight-bold primary--text">Bulk Uploads</v-card-title>
            <v-card-text>
              <v-file-input v-model="uploadFile" label="Upload File (CSV/Excel)" prepend-inner-icon="mdi-upload" accept=".csv,.xlsx" outlined rounded dense></v-file-input>
              <v-select v-model="uploadType" :items="uploadTypes" label="Upload Type" prepend-inner-icon="mdi-format-list-bulleted" outlined rounded dense></v-select>
              <v-btn color="primary" rounded @click="uploadFileHandler" :disabled="!uploadFile || !uploadType" class="gradient-btn"><v-icon left>mdi-cloud-upload</v-icon> Upload</v-btn>
            </v-card-text>
          </v-card>
        </v-tab-item>

        <!-- Maker-Checker Tab -->
        <v-tab-item>
          <v-card class="mt-6 pa-6 rounded-xl" elevation="6" color="grey lighten-4">
            <v-card-title class="text-h5 font-weight-bold primary--text">Maker-Checker Workflow</v-card-title>
            <v-card-text>
              <v-text-field v-model="makerCheckerLogId" label="Log ID" prepend-inner-icon="mdi-numeric" outlined rounded dense></v-text-field>
              <v-row justify="start">
                <v-btn color="success" rounded @click="confirmMakerChecker(true)" v-if="userRole === 'ROLE_APPROVER'" class="mr-4 gradient-btn"><v-icon left>mdi-check</v-icon> Approve</v-btn>
                <v-btn color="error" rounded @click="confirmMakerChecker(false)" v-if="userRole === 'ROLE_APPROVER'" class="gradient-btn"><v-icon left>mdi-close</v-icon> Reject</v-btn>
              </v-row>
            </v-card-text>
          </v-card>
        </v-tab-item>

        <!-- Reports Tab (Admin Only) -->
        <v-tab-item>
          <v-card class="mt-6 pa-6 rounded-xl" elevation="6" color="grey lighten-4">
            <v-card-title class="text-h5 font-weight-bold primary--text">Application Insights</v-card-title>
            <v-card-text>
              <v-data-table :headers="reportHeaders" :items="reportData" :loading="loading" class="elevation-2 rounded-lg" hide-default-footer></v-data-table>
              <v-btn color="primary" rounded class="mt-4 gradient-btn" @click="fetchReport"><v-icon left>mdi-refresh</v-icon> Refresh</v-btn>
            </v-card-text>
          </v-card>
        </v-tab-item>
      </v-container>
    </v-main>
  </v-app>
</template>

<script>
import axios from 'axios';

export default {
  data() {
    return {
      activeTab: 0,
      isAuthenticated: false,
      userRole: null,
      loginForm: { username: '', password: '' },
      applicantFilters: { name: '', idNumber: '', dateApplied: '' }, // Updated to object for multiple filters
      dateMenu: false, // For date picker
      applicants: [],
      applicantHeaders: [
        { text: 'ID', value: 'id' },
        { text: 'Name', value: 'fullName' },
        { text: 'National ID', value: 'idNumber' }, // Added National ID column
        { text: 'Status', value: 'verificationStatus' },
        { text: 'Actions', value: 'actions', sortable: false }
      ],
      applicationFilter: { status: '' },
      applications: [],
      applicationHeaders: [
        { text: 'ID', value: 'id' },
        { text: 'Applicant', value: 'applicant.fullName' },
        { text: 'Programme', value: 'programme.name' },
        { text: 'Status', value: 'status' },
        { text: 'Actions', value: 'actions', sortable: false }
      ],
      uploadFile: null,
      uploadType: null,
      uploadTypes: ['Applicants', 'Applications', 'Users'],
      makerCheckerLogId: '',
      reportData: [],
      reportHeaders: [
        { text: 'Programme', value: 'programmeName' },
        { text: 'Total', value: 'total' },
        { text: 'Approved', value: 'approved' },
        { text: 'Pending', value: 'pending' },
        { text: 'Rejected', value: 'rejected' }
      ],
      showApplicantForm: false,
      newApplicant: {
        firstName: '',
        middleName: '',
        lastName: '',
        sex: null,
        age: null,
        maritalStatus: null,
        idNumber: '',
        village: null,
        postalAddress: '',
        physicalAddress: '',
        telephone: ''
      },
      sexOptions: [],
      maritalStatusOptions: [],
      villageOptions: [],
      loading: false,
      tabs: ['Applicants', 'Applications', 'Uploads', 'Maker-Checker', 'Reports']
    };
  },
  created() {
    this.checkAuth();
  },
  methods: {
    checkAuth() {
      const token = localStorage.getItem('token');
      const role = localStorage.getItem('role');
      if (token && role) {
        this.isAuthenticated = true;
        this.userRole = role;
        axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
        this.fetchApplicants();
        this.fetchApplications();
        if (role === 'ROLE_ADMIN') this.fetchReport();
        if (role === 'ROLE_DATA_COLLECTOR') {
          this.fetchSexOptions();
          this.fetchMaritalStatusOptions();
          this.fetchVillages();
        }
      }
    },
    async login() {
      try {
        const response = await axios.post('/api/auth/login', this.loginForm);
        const { token, role } = response.data;
        localStorage.setItem('token', token);
        localStorage.setItem('role', role);
        this.isAuthenticated = true;
        this.userRole = role;
        axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
        this.fetchApplicants();
        this.fetchApplications();
        if (role === 'ROLE_ADMIN') this.fetchReport();
        if (role === 'ROLE_DATA_COLLECTOR') {
          this.fetchSexOptions();
          this.fetchMaritalStatusOptions();
          this.fetchVillages();
        }
      } catch (error) {
        alert('Login failed: ' + (error.response?.data?.message || error.message));
      }
    },
    logout() {
      localStorage.removeItem('token');
      localStorage.removeItem('role');
      delete axios.defaults.headers.common['Authorization'];
      this.isAuthenticated = false;
      this.userRole = null;
      this.applicants = [];
      this.applications = [];
      this.reportData = [];
    },
    async fetchApplicants() {
      this.loading = true;
      try {
        const response = await axios.get('/api/applicants/search', {
          params: {
            name: this.applicantFilters.name || null,
            idNumber: this.applicantFilters.idNumber || null,
            dateApplied: this.applicantFilters.dateApplied || null,
            size: 10,
            page: 0
          }
        });
        this.applicants = response.data.content.map(applicant => ({
          ...applicant,
          fullName: `${applicant.firstName} ${applicant.middleName || ''} ${applicant.lastName}`.trim()
        }));
      } catch (error) {
        alert('Failed to fetch applicants: ' + (error.response?.data?.message || error.message));
      }
      this.loading = false;
    },
    async deleteApplicant(id) {
      if (confirm('Are you sure you want to delete this applicant?')) {
        try {
          await axios.delete(`/api/applicants/${id}`);
          this.fetchApplicants();
        } catch (error) {
          alert('Failed to delete applicant: ' + (error.response?.data?.message || error.message));
        }
      }
    },
    async verifyApplicant(id) {
      try {
        await axios.patch(`/api/applicants/${id}/verify`, null, { params: { useMakerChecker: true } });
        this.fetchApplicants();
      } catch (error) {
        alert('Failed to verify applicant: ' + (error.response?.data?.message || error.message));
      }
    },
    async fetchApplications() {
      this.loading = true;
      try {
        const response = await axios.get('/api/applications/filter/status', { params: { status: this.applicationFilter.status, size: 10, page: 0 } });
        this.applications = response.data.content;
      } catch (error) {
        alert('Failed to fetch applications: ' + (error.response?.data?.message || error.message));
      }
      this.loading = false;
    },
    async deleteApplication(id) {
      if (confirm('Are you sure you want to delete this application?')) {
        try {
          await axios.delete(`/api/applications/${id}`);
          this.fetchApplications();
        } catch (error) {
          alert('Failed to delete application: ' + (error.response?.data?.message || error.message));
        }
      }
    },
    async approveApplication(id) {
      try {
        await axios.patch(`/api/applications/${id}/approve`, null, { params: { useMakerChecker: true } });
        this.fetchApplications();
      } catch (error) {
        alert('Failed to approve application: ' + (error.response?.data?.message || error.message));
      }
    },
    async uploadFileHandler() {
      this.loading = true;
      const formData = new FormData();
      formData.append('file', this.uploadFile);
      let endpoint = '';
      switch (this.uploadType) {
        case 'Applicants': endpoint = '/api/uploads/applicants'; break;
        case 'Applications': endpoint = '/api/uploads/applications'; break;
        case 'Users': endpoint = '/api/uploads/users'; break;
        default: alert('Invalid upload type'); this.loading = false; return;
      }
      try {
        await axios.post(endpoint, formData, { headers: { 'Content-Type': 'multipart/form-data' } });
        alert('File uploaded successfully');
        if (this.uploadType === 'Applicants') this.fetchApplicants();
        if (this.uploadType === 'Applications') this.fetchApplications();
      } catch (error) {
        alert('Upload failed: ' + (error.response?.data?.message || error.message));
      }
      this.loading = false;
      this.uploadFile = null;
      this.uploadType = null;
    },
    async confirmMakerChecker(approve) {
      if (!this.makerCheckerLogId) {
        alert('Please enter a Log ID');
        return;
      }
      try {
        await axios.post(`/api/maker-checker/logs/${this.makerCheckerLogId}/confirm`, null, { params: { approve } });
        this.fetchApplicants();
        this.fetchApplications();
        this.makerCheckerLogId = '';
      } catch (error) {
        alert('Failed to confirm maker-checker: ' + (error.response?.data?.message || error.message));
      }
    },
    async fetchReport() {
      this.loading = true;
      try {
        const response = await axios.get('/api/applications/report');
        this.reportData = response.data;
      } catch (error) {
        alert('Failed to fetch report: ' + (error.response?.data?.message || error.message));
      }
      this.loading = false;
    },
    async exportApplications() {
      try {
        const response = await axios.get('/api/applications/export', { params: { format: 'csv', status: this.applicationFilter.status }, responseType: 'blob' });
        const url = window.URL.createObjectURL(new Blob([response.data]));
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', 'applications.csv');
        document.body.appendChild(link);
        link.click();
        link.remove();
      } catch (error) {
        alert('Export failed: ' + (error.response?.data?.message || error.message));
      }
    },
    async fetchSexOptions() {
      try {
        const response = await axios.get('/api/parameters', { params: { category: 'Sex' } });
        this.sexOptions = response.data.content;
      } catch (error) {
        alert('Failed to fetch sex options: ' + (error.response?.data?.message || error.message));
      }
    },
    async fetchMaritalStatusOptions() {
      try {
        const response = await axios.get('/api/parameters', { params: { category: 'Marital Status' } });
        this.maritalStatusOptions = response.data.content;
      } catch (error) {
        alert('Failed to fetch marital status options: ' + (error.response?.data?.message || error.message));
      }
    },
    async fetchVillages() {
      try {
        const response = await axios.get('/api/villages', { params: { size: 1000, page: 0 } });
        this.villageOptions = response.data.content;
      } catch (error) {
        alert('Failed to fetch villages: ' + (error.response?.data?.message || error.message));
      }
    },
    async createApplicant() {
      if (this.$refs.applicantForm.validate()) {
        this.loading = true;
        try {
          const applicantData = {
            firstName: this.newApplicant.firstName,
            middleName: this.newApplicant.middleName || null,
            lastName: this.newApplicant.lastName,
            sex: { id: this.newApplicant.sex },
            age: this.newApplicant.age,
            maritalStatus: { id: this.newApplicant.maritalStatus },
            idNumber: this.newApplicant.idNumber,
            village: { id: this.newApplicant.village },
            postalAddress: this.newApplicant.postalAddress || null,
            physicalAddress: this.newApplicant.physicalAddress || null,
            telephone: this.newApplicant.telephone || null
          };
          await axios.post('/api/applicants', applicantData);
          alert('Applicant created successfully');
          this.showApplicantForm = false;
          this.$refs.applicantForm.reset();
          this.fetchApplicants();
        } catch (error) {
          alert('Failed to create applicant: ' + (error.response?.data?.message || error.message));
        }
        this.loading = false;
      }
    }
  }
};
</script>

<style scoped>
.background {
  background: linear-gradient(135deg, #e0f7fa 0%, #b2ebf2 100%);
  min-height: 100vh;
}

.gradient-bar {
  background: linear-gradient(45deg, #0288d1, #4fc3f7);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.gradient-btn {
  background: linear-gradient(45deg, #0288d1, #4fc3f7) !important;
  color: white !important;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.gradient-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.2);
}

.v-card {
  transition: transform 0.3s ease;
}

.v-card:hover {
  transform: translateY(-4px);
}

.v-tab {
  transition: background-color 0.3s ease;
}

.v-tab--active {
  background: linear-gradient(45deg, #0288d1, #4fc3f7);
  color: white !important;
}

.headline {
  font-family: 'Roboto', sans-serif;
  letter-spacing: 1px;
}
</style>
