package com.example.shop.service.impl;

import com.example.shop.entity.User;
import com.example.shop.entity.WorkShift;
import com.example.shop.entity.enums.EAccountStatus;
import com.example.shop.entity.enums.ShiftType;
import com.example.shop.exception.APIException;

import com.example.shop.payloads.reponse.WorkShiftPreviewResponse;
import com.example.shop.repository.UserRepo;
import com.example.shop.repository.WorkShiftRepo;
import com.example.shop.service.AutoWorkShiftService;
import lombok.AllArgsConstructor;


import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Transactional
public class  AutoWorkShiftServiceImpl implements AutoWorkShiftService {
    private final UserRepo userRepo;
    private final WorkShiftRepo workShiftRepo;
    @Autowired
    ModelMapper modelMapper;

    @Override
    public void autoSchedule(LocalDate from, LocalDate to, List<Long> userStaff, List<LocalDate> dates) {
        if (from.isAfter(to)) {
            throw new APIException("From must be before to");
        }
        LocalDate now=LocalDate.now();
        if(from.isAfter(now)){
            throw new APIException("Time begin must be better than current time");
        }
        if (dates != null) {
            for (LocalDate d : dates) {

                if (d.isBefore(LocalDate.now())) {
                    throw new APIException("Skip date must be >= today");
                }

                if (d.isBefore(from) || d.isAfter(to)) {
                    throw new APIException("Skip date must be inside schedule range");
                }

            }
        }
        Set<LocalDate> skip = dates == null
                ? Collections.emptySet()
                : new HashSet<>(dates);

        List<User> users = userRepo.findAllById(userStaff)
                .stream()
                .filter(u -> u.getAccountStatus() == EAccountStatus.ACTIVE).toList();
        if (users.isEmpty()) {
            throw new APIException("No active staff");
        }
        List<WorkShift> allShift = workShiftRepo.findByWorkDateBetween(from, to);
        Map<LocalDate, List<WorkShift>> shiftByDate = allShift
                .stream().
                collect(Collectors.groupingBy(WorkShift::getWorkDate));
        LocalDate date = from;
        while (!date.isAfter(to)) {
            if(skip.contains(date)){
                date=date.plusDays(1);
                continue;
            }
            List<WorkShift>existing=shiftByDate.getOrDefault(date,new ArrayList<>());
            processDay(date,users,existing);
            date = date.plusDays(1);
        }
    }

    //quy trình ngày
    public void processDay(LocalDate date,List<User>staff,List<WorkShift>existing){
        Map<ShiftType,Long>shiftCount=buildShiftCount(existing);
        Map<Long,List<ShiftType>>staffShiftMap=buildStaffShiftMap(existing);

        List<User> availableStaff=staff.stream().filter(s->staffShiftMap
                .getOrDefault(s.getUserId(),Collections.emptyList()).size()<=2).toList();
        List<WorkShift>newWorkShift=new ArrayList<>();
        for(User u:availableStaff){
            ShiftType shift=chooseBestShift(u,shiftCount,staffShiftMap);
            if(shift==null){
                continue;
            }
            WorkShift ws=new WorkShift();
            ws.setStaff(u);
            ws.setWorkDate(date);
            ws.setShiftType(shift);
            newWorkShift.add(ws);

            shiftCount.put(shift,shiftCount.getOrDefault(shift,0L)+1);
            staffShiftMap.computeIfAbsent(u.getUserId(),k->new ArrayList<>()).add(shift);

        }
        if(!newWorkShift.isEmpty()){
            workShiftRepo.saveAll(newWorkShift);
        }
    }

    //xây dựng bản đồ làm việc ca
    public Map<Long,List<ShiftType>> buildStaffShiftMap(List<WorkShift>shifts){
        Map<Long,List<ShiftType>>map=new HashMap<>();
        for(WorkShift ws:shifts){
            Long staffId=ws.getStaff().getUserId();
            List<ShiftType>shiftTypes=map.get(staffId);

            if(shiftTypes==null){
                shiftTypes=new ArrayList<>();
                map.put(staffId,shiftTypes);
            }
            shiftTypes.add(ws.getShiftType());
        }
        return map;
    }

    //xây dựng số ca làm việc
    private Map<ShiftType, Long>buildShiftCount(List<WorkShift>shifts){

        Map<ShiftType,Long>map=shifts
                .stream()
                .collect(Collectors.groupingBy(WorkShift::getShiftType,Collectors.counting()));
        for(ShiftType type:ShiftType.values()){
            map.putIfAbsent(type,0L);
        }
        return map;
    }
    private ShiftType chooseBestShift(
            User staff,
            Map<ShiftType,Long>countShiftType,
            Map<Long,List<ShiftType>>staffShiftMap
    ){
        List<ShiftType>work=staffShiftMap.getOrDefault(staff.getUserId(),Collections.emptyList());
        if(work.size()>=2){
            return null;
        }
        List<ShiftType>candidates=new ArrayList<>(List.of(ShiftType.values()));
        if(work.contains(ShiftType.MORNING)){
            candidates.remove(ShiftType.EVENING);
        }
        if(work.contains(ShiftType.EVENING)){
            candidates.remove(ShiftType.MORNING);
        }
        return candidates.stream().min(Comparator.comparing(s-> countShiftType.getOrDefault(s,0L))).
                orElse(null);
    }


    /// xem lịch phân tự động
    @Override
    public List<WorkShiftPreviewResponse>previewSchedule(LocalDate from, LocalDate to, List<Long>userStaffId, List<LocalDate>dates){
        if(from.isAfter(to)){
            throw new APIException("From must be before to");
        }
        LocalDate now=LocalDate.now();
        if(from.isAfter(now)){
            throw new APIException("Time begin must be better than current time");
        }
        if (dates != null) {
            for (LocalDate d : dates) {

                if (d.isBefore(LocalDate.now())) {
                    throw new APIException("Skip date must be >= today");
                }

                if (d.isBefore(from) || d.isAfter(to)) {
                    throw new APIException("Skip date must be inside schedule range");
                }

            }
        }

        Set<LocalDate> skip = dates == null
                ? Collections.emptySet()
                : new HashSet<>(dates);
        List<User>users=userRepo.findAllById(userStaffId).stream().filter(u->u.getAccountStatus()==EAccountStatus.ACTIVE).toList();
        List<WorkShift>result=new ArrayList<>();
        LocalDate date=from;
        while(!date.isAfter(to)){
            if(skip.contains(date)){
                date=date.plusDays(1);
                continue;
            }
            List<WorkShift>shiftAllDay=processDayPreview(date,users,new ArrayList<>());
            result.addAll(shiftAllDay);

            date=date.plusDays(1);
        }
        return result.stream().map(ws->{
            WorkShiftPreviewResponse dto=new WorkShiftPreviewResponse();
            dto.setStaffId(ws.getWorkShiftId());
            dto.setStaffName(ws.getStaff().getUserName());
            dto.setWorkDate(ws.getWorkDate());
            dto.setShiftType(ws.getShiftType());
            return dto;
        }).toList();

    }

    public List<WorkShift>processDayPreview(
            LocalDate date,
            List<User>staff,
            List<WorkShift>existing
    ){
        Map<ShiftType,Long>shiftCount=buildShiftCount(existing);
        Map<Long,List<ShiftType>>staffShiftMap=buildStaffShiftMap(existing);
        List<User>availableStaff=staff.stream().filter(
                s->staffShiftMap.getOrDefault(s.getUserId(),Collections.emptyList()).size()<2).toList();
        List<WorkShift>newWorkShift=new ArrayList<>();
        for(User u:availableStaff){
            ShiftType shift=chooseBestShift(u,shiftCount,staffShiftMap);
            if(shift==null){
                continue;
            }
            WorkShift ws=new WorkShift();
            ws.setStaff(u);
            ws.setWorkDate(date);
            ws.setShiftType(shift);
            newWorkShift.add(ws);

            shiftCount.put(shift,shiftCount.getOrDefault(shift,0L)+1);
            staffShiftMap.computeIfAbsent(u.getUserId(),k->new ArrayList<>()).add(shift);

        }
        return newWorkShift;
    }

}
