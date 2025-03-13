package com.social.assistance.service;

import com.social.assistance.exception.InvalidFileException;
import com.social.assistance.exception.ResourceNotFoundException;
import com.social.assistance.model.*;
import com.social.assistance.repository.*;
import com.opencsv.CSVReader;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UploadService {

    private final ApplicantRepository applicantRepository;
    private final ApplicationRepository applicationRepository;
    private final ProgrammeRepository programmeRepository;
    private final ParameterRepository parameterRepository;
    private final VillageRepository villageRepository;
    private final SubLocationRepository subLocationRepository;
    private final LocationRepository locationRepository;
    private final SubCountyRepository subCountyRepository;
    private final CountyRepository countyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService; // Added for consistency

    private static final String UPLOAD_DIR = "uploads/";

    @PersistenceContext
    private EntityManager entityManager; // For stored procedures (optional, unused here)

    @PreAuthorize("hasRole('DATA_COLLECTOR')")
    @Transactional
    public List<Applicant> uploadApplicants(MultipartFile file) {
        validateFile(file, "csv", "xlsx");
        List<Applicant> applicants = parseApplicants(file);
        return applicantRepository.saveAll(applicants);
    }

    @PreAuthorize("hasRole('DATA_COLLECTOR')")
    @Transactional
    public List<Application> uploadApplications(MultipartFile file) {
        validateFile(file, "csv", "xlsx");
        List<Application> applications = parseApplications(file);
        return applicationRepository.saveAll(applications);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public List<Parameter> uploadParameters(MultipartFile file) {
        validateFile(file, "csv", "xlsx");
        List<Parameter> parameters = parseParameters(file);
        return parameterRepository.saveAll(parameters);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void uploadPhysicalLocations(MultipartFile file) {
        validateFile(file, "csv", "xlsx");
        parseAndSavePhysicalLocations(file);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public List<User> uploadUsers(MultipartFile file) {
        validateFile(file, "csv", "xlsx");
        List<User> users = parseUsers(file);
        return userRepository.saveAll(users);
    }

    @PreAuthorize("hasRole('DATA_COLLECTOR')")
    @Transactional
    public String uploadApplicantImage(Integer applicantId, MultipartFile image) {
        validateImage(image);
        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new ResourceNotFoundException("Applicant not found with ID: " + applicantId));
        String filePath = saveFile(image, "images/" + applicantId + "/");
        applicant.setImagePath(filePath);
        applicantRepository.save(applicant);
        return filePath;
    }

    private void validateFile(MultipartFile file, String... allowedExtensions) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File cannot be empty");
        }
        String fileName = Objects.requireNonNull(file.getOriginalFilename()).toLowerCase();
        boolean valid = false;
        for (String ext : allowedExtensions) {
            if (fileName.endsWith("." + ext)) {
                valid = true;
                break;
            }
        }
        if (!valid) {
            throw new InvalidFileException("Only CSV and Excel (.csv, .xlsx) files are allowed");
        }
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new InvalidFileException("Image cannot be empty");
        }
        String fileName = Objects.requireNonNull(image.getOriginalFilename()).toLowerCase();
        if (!fileName.endsWith(".jpg") && !fileName.endsWith(".jpeg") && !fileName.endsWith(".png")) {
            throw new InvalidFileException("Only JPG, JPEG, and PNG images are allowed");
        }
    }

    private String saveFile(MultipartFile file, String subDir) {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR + subDir);
            Files.createDirectories(uploadPath);
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.write(filePath, file.getBytes());
            return filePath.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file", e);
        }
    }

    private List<Applicant> parseApplicants(MultipartFile file) {
        List<Applicant> applicants = new ArrayList<>();
        if (Objects.requireNonNull(file.getOriginalFilename()).endsWith(".csv")) {
            try (CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
                String[] headers = csvReader.readNext(); // Expected headers: firstName, middleName, lastName, sexId, age, maritalStatusId, idNumber, villageId, postalAddress, physicalAddress, telephone
                if (headers == null || headers.length < 11) {
                    throw new InvalidFileException("CSV file must have at least 11 columns: firstName, middleName, lastName, sexId, age, maritalStatusId, idNumber, villageId, postalAddress, physicalAddress, telephone");
                }
                String[] line;
                while ((line = csvReader.readNext()) != null) {
                    if (line.length < 11) {
                        continue; // Skip malformed rows
                    }
                    Applicant applicant = new Applicant();
                    applicant.setFirstName(line[0].trim());
                    applicant.setMiddleName(line[1].trim().isEmpty() ? null : line[1].trim());
                    applicant.setLastName(line[2].trim());
                    String[] finalLine = line;
                    applicant.setSex(parameterRepository.findById(Integer.parseInt(line[3].trim()))
                            .orElseThrow(() -> new ResourceNotFoundException("Sex parameter not found: " + finalLine[3])));
                    applicant.setAge(Integer.parseInt(line[4].trim()));
                    String[] finalLine1 = line;
                    applicant.setMaritalStatus(parameterRepository.findById(Integer.parseInt(line[5].trim()))
                            .orElseThrow(() -> new ResourceNotFoundException("Marital status not found: " + finalLine1[5])));
                    applicant.setIdNumber(line[6].trim());
                    String[] finalLine2 = line;
                    applicant.setVillage(villageRepository.findById(Integer.parseInt(line[7].trim()))
                            .orElseThrow(() -> new ResourceNotFoundException("Village not found: " + finalLine2[7])));
                    applicant.setPostalAddress(line[8].trim().isEmpty() ? null : line[8].trim());
                    applicant.setPhysicalAddress(line[9].trim().isEmpty() ? null : line[9].trim());
                    applicant.setTelephone(line[10].trim().isEmpty() ? null : line[10].trim());
                    applicant.setVerificationStatus("Pending"); // Default value
                    applicants.add(applicant);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse CSV for applicants", e);
            }
        } else { // Excel (.xlsx)
            try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
                Sheet sheet = workbook.getSheetAt(0);
                Row headerRow = sheet.getRow(0);
                if (headerRow == null || headerRow.getLastCellNum() < 11) {
                    throw new InvalidFileException("Excel file must have at least 11 columns: firstName, middleName, lastName, sexId, age, maritalStatusId, idNumber, villageId, postalAddress, physicalAddress, telephone");
                }
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null || row.getLastCellNum() < 11) {
                        continue; // Skip malformed rows
                    }
                    Applicant applicant = new Applicant();
                    applicant.setFirstName(row.getCell(0).getStringCellValue().trim());
                    String middleName = row.getCell(1).getStringCellValue().trim();
                    applicant.setMiddleName(middleName.isEmpty() ? null : middleName);
                    applicant.setLastName(row.getCell(2).getStringCellValue().trim());
                    applicant.setSex(parameterRepository.findById((int) row.getCell(3).getNumericCellValue())
                            .orElseThrow(() -> new ResourceNotFoundException("Sex parameter not found")));
                    applicant.setAge((int) row.getCell(4).getNumericCellValue());
                    applicant.setMaritalStatus(parameterRepository.findById((int) row.getCell(5).getNumericCellValue())
                            .orElseThrow(() -> new ResourceNotFoundException("Marital status not found")));
                    applicant.setIdNumber(row.getCell(6).getStringCellValue().trim());
                    applicant.setVillage(villageRepository.findById((int) row.getCell(7).getNumericCellValue())
                            .orElseThrow(() -> new ResourceNotFoundException("Village not found")));
                    String postalAddress = row.getCell(8).getStringCellValue().trim();
                    applicant.setPostalAddress(postalAddress.isEmpty() ? null : postalAddress);
                    String physicalAddress = row.getCell(9).getStringCellValue().trim();
                    applicant.setPhysicalAddress(physicalAddress.isEmpty() ? null : physicalAddress);
                    String telephone = row.getCell(10).getStringCellValue().trim();
                    applicant.setTelephone(telephone.isEmpty() ? null : telephone);
                    applicant.setVerificationStatus("Pending"); // Default value
                    applicants.add(applicant);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse Excel for applicants", e);
            }
        }
        if (applicants.isEmpty()) {
            throw new InvalidFileException("No valid applicant data found in the file");
        }
        return applicants;
    }

    private List<Application> parseApplications(MultipartFile file) {
        List<Application> applications = new ArrayList<>();
        if (Objects.requireNonNull(file.getOriginalFilename()).endsWith(".csv")) {
            try (CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
                String[] headers = csvReader.readNext(); // Expected headers: firstName, middleName, lastName, programmeName, applicationDate
                if (headers == null || headers.length < 5) {
                    throw new InvalidFileException("CSV file must have at least 5 columns: firstName, middleName, lastName, programmeName, applicationDate");
                }
                String[] line;
                while ((line = csvReader.readNext()) != null) {
                    if (line.length < 5) {
                        continue; // Skip malformed rows
                    }
                    String firstName = line[0].trim();
                    String middleName = line[1].trim().isEmpty() ? null : line[1].trim();
                    String lastName = line[2].trim();
                    String programmeName = line[3].trim();
                    String applicationDateStr = line[4].trim();

                    Applicant applicant = applicantRepository.findByFirstNameAndLastNameAndMiddleName(firstName, lastName, middleName)
                            .orElseThrow(() -> new ResourceNotFoundException("Applicant not found: " + firstName + " " + (middleName != null ? middleName + " " : "") + lastName));
                    Programme programme = programmeRepository.findByName(programmeName)
                            .orElseThrow(() -> new ResourceNotFoundException("Programme not found: " + programmeName));

                    Application application = new Application();
                    application.setApplicant(applicant);
                    application.setProgramme(programme);
                    application.setApplicationDate(LocalDate.parse(applicationDateStr)); // Assumes ISO format (e.g., 2023-01-01)
                    application.setStatus("Pending"); // Default value
                    applications.add(application);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse CSV for applications", e);
            }
        } else { // Excel (.xlsx)
            try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
                Sheet sheet = workbook.getSheetAt(0);
                Row headerRow = sheet.getRow(0);
                if (headerRow == null || headerRow.getLastCellNum() < 5) {
                    throw new InvalidFileException("Excel file must have at least 5 columns: firstName, middleName, lastName, programmeName, applicationDate");
                }
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null || row.getLastCellNum() < 5) {
                        continue; // Skip malformed rows
                    }
                    String firstName = row.getCell(0).getStringCellValue().trim();
                    String middleName = row.getCell(1).getStringCellValue().trim();
                    middleName = middleName.isEmpty() ? null : middleName;
                    String lastName = row.getCell(2).getStringCellValue().trim();
                    String programmeName = row.getCell(3).getStringCellValue().trim();
                    String applicationDateStr = row.getCell(4).getStringCellValue().trim();

                    String finalMiddleName = middleName;
                    Applicant applicant = applicantRepository.findByFirstNameAndLastNameAndMiddleName(firstName, lastName, middleName)
                            .orElseThrow(() -> new ResourceNotFoundException("Applicant not found: " + firstName + " " + (finalMiddleName != null ? finalMiddleName + " " : "") + lastName));
                    Programme programme = programmeRepository.findByName(programmeName)
                            .orElseThrow(() -> new ResourceNotFoundException("Programme not found: " + programmeName));

                    Application application = new Application();
                    application.setApplicant(applicant);
                    application.setProgramme(programme);
                    application.setApplicationDate(LocalDate.parse(applicationDateStr)); // Assumes ISO format (e.g., 2023-01-01)
                    application.setStatus("Pending"); // Default value
                    applications.add(application);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse Excel for applications", e);
            }
        }
        if (applications.isEmpty()) {
            throw new InvalidFileException("No valid application data found in the file");
        }
        return applications;
    }

    private List<Parameter> parseParameters(MultipartFile file) {
        List<Parameter> parameters = new ArrayList<>();
        if (file.getOriginalFilename().endsWith(".csv")) {
            try (CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
                String[] headers = csvReader.readNext(); // Expected headers: category, value
                if (headers == null || headers.length < 2) {
                    throw new InvalidFileException("CSV file must have at least 2 columns: category, value");
                }
                String[] line;
                while ((line = csvReader.readNext()) != null) {
                    if (line.length < 2) {
                        continue; // Skip malformed rows
                    }
                    String category = line[0].trim();
                    String value = line[1].trim();

                    Parameter parameter = new Parameter();
                    parameter.setCategory(category);
                    parameter.setValue(value);
                    parameters.add(parameter);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse CSV for parameters", e);
            }
        } else { // Excel (.xlsx)
            try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
                Sheet sheet = workbook.getSheetAt(0);
                Row headerRow = sheet.getRow(0);
                if (headerRow == null || headerRow.getLastCellNum() < 2) {
                    throw new InvalidFileException("Excel file must have at least 2 columns: category, value");
                }
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null || row.getLastCellNum() < 2) {
                        continue; // Skip malformed rows
                    }
                    String category = row.getCell(0).getStringCellValue().trim();
                    String value = row.getCell(1).getStringCellValue().trim();

                    Parameter parameter = new Parameter();
                    parameter.setCategory(category);
                    parameter.setValue(value);
                    parameters.add(parameter);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse Excel for parameters", e);
            }
        }
        if (parameters.isEmpty()) {
            throw new InvalidFileException("No valid parameter data found in the file");
        }
        return parameters;
    }

    private void parseAndSavePhysicalLocations(MultipartFile file) {
        if (file.getOriginalFilename().endsWith(".csv")) {
            try (CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
                String[] headers = csvReader.readNext(); // Expected headers: countyName, subCountyName, locationName, subLocationName, villageName
                if (headers == null || headers.length < 5) {
                    throw new InvalidFileException("CSV file must have 5 columns: countyName, subCountyName, locationName, subLocationName, villageName");
                }
                String[] line;
                while ((line = csvReader.readNext()) != null) {
                    if (line.length < 5) {
                        continue; // Skip malformed rows
                    }
                    String countyName = line[0].trim();
                    String subCountyName = line[1].trim();
                    String locationName = line[2].trim();
                    String subLocationName = line[3].trim();
                    String villageName = line[4].trim();

                    // County
                    County county = countyRepository.findByName(countyName)
                            .orElseGet(() -> {
                                County newCounty = new County();
                                newCounty.setName(countyName);
                                return countyRepository.save(newCounty);
                            });

                    // SubCounty
                    SubCounty subCounty = subCountyRepository.findByNameAndCounty(subCountyName, county)
                            .orElseGet(() -> {
                                SubCounty newSubCounty = new SubCounty();
                                newSubCounty.setName(subCountyName);
                                newSubCounty.setCounty(county);
                                return subCountyRepository.save(newSubCounty);
                            });

                    // Location
                    Location location = locationRepository.findByNameAndSubCounty(locationName, subCounty)
                            .orElseGet(() -> {
                                Location newLocation = new Location();
                                newLocation.setName(locationName);
                                newLocation.setSubCounty(subCounty);
                                return locationRepository.save(newLocation);
                            });

                    // SubLocation
                    SubLocation subLocation = subLocationRepository.findByNameAndLocation(subLocationName, location)
                            .orElseGet(() -> {
                                SubLocation newSubLocation = new SubLocation();
                                newSubLocation.setName(subLocationName);
                                newSubLocation.setLocation(location);
                                return subLocationRepository.save(newSubLocation);
                            });

                    // Village
                    Village village = villageRepository.findByNameAndSubLocation(villageName, subLocation)
                            .orElseGet(() -> {
                                Village newVillage = new Village();
                                newVillage.setName(villageName);
                                newVillage.setSubLocation(subLocation);
                                return villageRepository.save(newVillage);
                            });
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse CSV for physical locations", e);
            }
        } else { // Excel (.xlsx)
            try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
                Sheet sheet = workbook.getSheetAt(0);
                Row headerRow = sheet.getRow(0);
                if (headerRow == null || headerRow.getLastCellNum() < 5) {
                    throw new InvalidFileException("Excel file must have 5 columns: countyName, subCountyName, locationName, subLocationName, villageName");
                }
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null || row.getLastCellNum() < 5) {
                        continue; // Skip malformed rows
                    }
                    String countyName = row.getCell(0).getStringCellValue().trim();
                    String subCountyName = row.getCell(1).getStringCellValue().trim();
                    String locationName = row.getCell(2).getStringCellValue().trim();
                    String subLocationName = row.getCell(3).getStringCellValue().trim();
                    String villageName = row.getCell(4).getStringCellValue().trim();

                    // County
                    County county = countyRepository.findByName(countyName)
                            .orElseGet(() -> {
                                County newCounty = new County();
                                newCounty.setName(countyName);
                                return countyRepository.save(newCounty);
                            });

                    // SubCounty
                    SubCounty subCounty = subCountyRepository.findByNameAndCounty(subCountyName, county)
                            .orElseGet(() -> {
                                SubCounty newSubCounty = new SubCounty();
                                newSubCounty.setName(subCountyName);
                                newSubCounty.setCounty(county);
                                return subCountyRepository.save(newSubCounty);
                            });

                    // Location
                    Location location = locationRepository.findByNameAndSubCounty(locationName, subCounty)
                            .orElseGet(() -> {
                                Location newLocation = new Location();
                                newLocation.setName(locationName);
                                newLocation.setSubCounty(subCounty);
                                return locationRepository.save(newLocation);
                            });

                    // SubLocation
                    SubLocation subLocation = subLocationRepository.findByNameAndLocation(subLocationName, location)
                            .orElseGet(() -> {
                                SubLocation newSubLocation = new SubLocation();
                                newSubLocation.setName(subLocationName);
                                newSubLocation.setLocation(location);
                                return subLocationRepository.save(newSubLocation);
                            });

                    // Village
                    Village village = villageRepository.findByNameAndSubLocation(villageName, subLocation)
                            .orElseGet(() -> {
                                Village newVillage = new Village();
                                newVillage.setName(villageName);
                                newVillage.setSubLocation(subLocation);
                                return villageRepository.save(newVillage);
                            });
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse Excel for physical locations", e);
            }
        }
    }

    private List<User> parseUsers(MultipartFile file) {
        List<User> users = new ArrayList<>();
        if (file.getOriginalFilename().endsWith(".csv")) {
            try (CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
                String[] headers = csvReader.readNext(); // Expected headers: username, password, name, role, email, phone
                if (headers == null || headers.length < 6) {
                    throw new InvalidFileException("CSV file must have at least 6 columns: username, password, name, role, email, phone");
                }
                String[] line;
                while ((line = csvReader.readNext()) != null) {
                    if (line.length < 6) {
                        continue; // Skip malformed rows
                    }
                    String username = line[0].trim();
                    String password = line[1].trim();
                    String name = line[2].trim();
                    String role = line[3].trim();
                    String email = line[4].trim().isEmpty() ? null : line[4].trim();
                    String phone = line[5].trim().isEmpty() ? null : line[5].trim();

                    User user = new User();
                    user.setUsername(username);
                    user.setPassword(passwordEncoder.encode(password));
                    user.setName(name);
                    user.setRole(role);
                    user.setEmail(email);
                    user.setPhone(phone);
                    user.setEnabled(true); // Default value
                    user.setCreatedAt(LocalDateTime.now()); // Default value
                    user.setUpdatedAt(LocalDateTime.now()); // Default value
                    users.add(user);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse CSV for users", e);
            }
        } else { // Excel (.xlsx)
            try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
                Sheet sheet = workbook.getSheetAt(0);
                Row headerRow = sheet.getRow(0);
                if (headerRow == null || headerRow.getLastCellNum() < 6) {
                    throw new InvalidFileException("Excel file must have at least 6 columns: username, password, name, role, email, phone");
                }
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null || row.getLastCellNum() < 6) {
                        continue; // Skip malformed rows
                    }
                    String username = row.getCell(0).getStringCellValue().trim();
                    String password = row.getCell(1).getStringCellValue().trim();
                    String name = row.getCell(2).getStringCellValue().trim();
                    String role = row.getCell(3).getStringCellValue().trim();
                    String email = row.getCell(4).getStringCellValue().trim();
                    email = email.isEmpty() ? null : email;
                    String phone = row.getCell(5).getStringCellValue().trim();
                    phone = phone.isEmpty() ? null : phone;

                    User user = new User();
                    user.setUsername(username);
                    user.setPassword(passwordEncoder.encode(password));
                    user.setName(name);
                    user.setRole(role);
                    user.setEmail(email);
                    user.setPhone(phone);
                    user.setEnabled(true); // Default value
                    user.setCreatedAt(LocalDateTime.now()); // Default value
                    user.setUpdatedAt(LocalDateTime.now()); // Default value
                    users.add(user);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse Excel for users", e);
            }
        }
        if (users.isEmpty()) {
            throw new InvalidFileException("No valid user data found in the file");
        }
        return users;
    }
}
