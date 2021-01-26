package nextstep.subway.path.domain;

import nextstep.subway.line.domain.Line;
import nextstep.subway.line.domain.Section;
import nextstep.subway.station.domain.Station;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.WeightedMultigraph;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PathFinder {
    private List<Long> shortestPath = new ArrayList<>();
    private Integer distance;
    private Integer totalFee;
    private int maxAddFee;

    public PathFinder() {
    }

    public void findRouteSearch(Station station1, Station station2, List<Line> lines, Integer age) {
        if (station1.equals(station2)) {
            throw new IllegalArgumentException("출발역과 도착역이 같습니다!");
        }

        Set<Station> vertex = new HashSet<>();
        List<Section> edge = new ArrayList<>();
        for(Line line: lines) {
            extractVertex(vertex, line);
            extracteEdge(edge, line);
        }

        if (!vertex.contains(station1) || !vertex.contains(station2)) {
            throw new IllegalArgumentException("존재하지 않은 출발역이나 도착역입니다!");
        }

        //최단경로 구하기
        distance = getDijkstraShortestPath(station1.getId(), station2.getId(), vertex, edge);
        if (distance == 0) {
            throw new IllegalArgumentException("출발역과 도착역이 연결되지 않았습니다!");
        }
        
        //지하철 이용 요금 구하기
        findTotalFee(lines, age);
    }

    private void extracteEdge(List<Section> edge, Line line) {
        for(Section section: line.getSections()) {
            edge.add(section);
        }
    }

    private void extractVertex(Set<Station> vertex, Line line) {
        for(Station station: line.getStations(line)) {
            vertex.add(station);
        }
    }

    public Integer getDijkstraShortestPath(Long source, Long target, Set<Station> vertex, List<Section> edge) {
        WeightedMultigraph<Long, DefaultWeightedEdge> graph
                = new WeightedMultigraph(DefaultWeightedEdge.class);
        for(Station station: vertex) {
            graph.addVertex(station.getId());
        }
        for(Section section: edge) {
            graph.setEdgeWeight(graph.addEdge(section.getUpStation().getId(),
                    section.getDownStation().getId()), section.getDistance());
        }

        DijkstraShortestPath dijkstraShortestPath = new DijkstraShortestPath(graph);

        shortestPath = dijkstraShortestPath.getPath(source, target).getVertexList();

        double shortDistance
                = dijkstraShortestPath.getPath(source, target).getWeight();

        return Integer.parseInt(String.valueOf(Math.round(shortDistance)));
    }

    public void findTotalFee(List<Line> lines, Integer age) {
        baseFare(distance);
        //노선별 추가 요금 정책(추가요금이 있는 노선을 환승하여 이용할 경우, 가장 높은 금액의 추가요금만 적용)
        for(Line line : lines) {
            lineAddFee(line);
        }
        totalFee = totalFee + maxAddFee;
        //로그인 사용자 연령별 요금 할인 적용(청소년 13세이상 ~19세 미만, 어린이 6세이상 ~13세 미만)
        ageDiscountFee(age);
    }

    public void ageDiscountFee(int age){
        int targetFee = 0;
        for (AgeDiscount ageDiscount: AgeDiscount.values()) {
            if (age >= ageDiscount.getStartAge() && age <= ageDiscount.getEndAge()) {
                targetFee = totalFee - ageDiscount.getDeductionFare();
                totalFee = targetFee - (int)Math.floor(targetFee * ageDiscount.getDeductionRatio());
            }
        }
    }

    public void baseFare(int distance) {
        if (distance <= 10) {
            totalFee = 1250;
        }

        if (distance > 10 && distance <= 50) {
            totalFee = 1250 + calculateOverFare(5, distance-10);
        }

        if (distance > 50) {
            totalFee = 2050 + calculateOverFare(8, distance-50);
        }
    }

    public int calculateOverFare(int unit, int distance) {
        for(OverFare overFare: OverFare.values()){
            if (unit == overFare.getUnit() && distance % overFare.getUnit() == 0) {
                return (int) ((Math.floor((distance - 1) / overFare.getUnit()) + 1) * 100);
            }
            if (unit == overFare.getUnit() && distance % overFare.getUnit()  != 0) {
                return (int) (Math.floor((distance - 1) / overFare.getUnit()) * 100);
            }
        }
        return 0;
    }

    private void lineAddFee(Line line) {
        if (line.containsPath(shortestPath)) {
            extractedMaxAddFee(line);
        }
    }

    private void extractedMaxAddFee(Line line) {
        if (maxAddFee < line.getAddFee()){
            maxAddFee = line.getAddFee();
        }
    }

    public List<Long> getStation() {
        return shortestPath;
    }

    public int getDistance() {
        return distance;
    }

    public Integer getTotalFee() {
        return totalFee;
    }
}

