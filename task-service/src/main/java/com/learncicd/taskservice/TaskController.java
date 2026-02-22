package com.learncicd.taskservice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@Slf4j
@RequiredArgsConstructor
public class TaskController {


    private final TaskService taskService;

    @GetMapping("/health")
    public String getTaskHealth() {
        log.info("API Request: Get Task Health");
        return "TASK-UP-Healthy";
    }

    @PostMapping("/save")
    public ResponseEntity<Task> createTask(@RequestBody Task task) {
        log.info("API Request: Create Task for userId={}", task.getUserId());
        Task responseTask = taskService.createTask(task);
        return new ResponseEntity<>(responseTask, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> getTask(@PathVariable Long id) {
        log.info("API Request: Get Task id={}", id);
        Task responseTask = taskService.getTask(id);
        return new ResponseEntity<>(responseTask, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<Iterable<Task>> getAllTasks() {
        log.info("API Request: Get All Tasks");
        Iterable<Task> responseTasks = taskService.getAllTasks();
        return new ResponseEntity<>(responseTasks,HttpStatus.OK);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Iterable<Task>> getTasksByUser(@PathVariable Long userId) {
        log.info("API Request: Get Tasks for userId={}", userId);
        Iterable<Task> responseTasks = taskService.getTasksByUser(userId);
        return new ResponseEntity<>(responseTasks,HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable Long id, @RequestBody Task task) {
        log.info("API Request: Update Task id={}", id);
        Task updatedTask = taskService.updateTask(id, task);
        return new ResponseEntity<>(updatedTask, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTask(@PathVariable Long id) {
        log.info("API Request: Delete Task id={}", id);
        taskService.deleteTask(id);
        return new ResponseEntity<>("Task deleted successfully", HttpStatus.OK);
    }
}

