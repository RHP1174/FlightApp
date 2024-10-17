package flightapp;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Runs queries against a back-end database
 */
public class Query extends QueryAbstract {
  //
  // Canned queries
  //
  private static final String FLIGHT_CAPACITY_SQL = "SELECT capacity FROM Flights WHERE fid = ?";
  private PreparedStatement flightCapacityStmt;

  private static final String CLEAR_TABLES_SQL =
  "DELETE FROM RESERVATIONS_raj1174; " +
  "DELETE FROM USERS_raj1174";
  private PreparedStatement tablesClearStmt;

  private static final String CHECK_USER_SQL = "SELECT * FROM USERS_raj1174 WHERE username = ?";
  private PreparedStatement userCheckStmt;

  private static final String USER_CREATE_SQL = "INSERT INTO USERS_raj1174 VALUES(?, ?, ?)";
  private PreparedStatement userCreateStmt;

  private static String SEARCH_DIRECT_SQL =
  "SELECT DISTINCT * " +
  "FROM FLIGHTS AS f " +
  "WHERE f.origin_city = ? " +
  "AND f.dest_city = ? " +
  "AND f.day_of_month = ? " +
  "AND f.canceled = 0 " +
  "ORDER BY f.actual_time, f.fid";
  private PreparedStatement searchDirectFlightStmt;

  private static String SEARCH_INDIRECT_SQL =
  "SELECT DISTINCT *, f.actual_time + f2.actual_time AS total_flight_time " +
  "FROM FLIGHTS AS f, FLIGHTS AS f2 " +
  "WHERE f.origin_city = ? " +
  "AND f2.dest_city = ? " +
  "AND f.dest_city = f2.origin_city " +
  "AND f.dest_city != f2.dest_city " +
  "AND f.canceled = 0 " +
  "AND f2.canceled = 0 " +
  "AND f.day_of_month = ? " +
  "AND f2.day_of_month = ? " +
  "ORDER BY total_flight_time, f.fid, f2.fid";
  private PreparedStatement searchIndirectFlightStmt;

  private static String CHECK_USER_RESERVATION_SQL =
  "SELECT * " +
  "FROM FLIGHTS AS f, RESERVATIONS_raj1174 AS r " +
  "WHERE r.username = ? " +
  "AND f.fid = r.fid";
  private PreparedStatement userReservationCheckStmt;

  private static final String RESERVATION_FLIGHT_COUNT_CHECK_SQL =
  "SELECT COUNT(*) " +
  "FROM RESERVATIONS_raj1174 AS r " +
  "WHERE r.fid = ? OR r.fid2 = ? ";
  private PreparedStatement reservationCountCheckStmt;

  private static final String NUM_OF_RESERVATIONS_SQL =
  "SELECT COUNT(*) " +
  "FROM RESERVATIONS_raj1174";
  private PreparedStatement numOfReservationsStmt;

  private static final String CREATE_RESERVATION_SQL =
  "INSERT INTO RESERVATIONS_raj1174 (username, rid, fid, fid2, paid) " +
  "VALUES(?, ?, ?, ?, ?)";
  private PreparedStatement createReservationStmt;

  private static final String FIND_USER_RESERVATION_SQL =
  "SELECT * " +
  "FROM RESERVATIONS_raj1174 " +
  "WHERE username = ? " +
  "AND rid = ? ";
  private PreparedStatement findReservationStmt;

  private static final String FIND_FLIGHT_ID_SQL =
  "SELECT * " +
  "FROM FLIGHTS " +
  "WHERE fid = ? ";
  private PreparedStatement findFlightStmt;

  private static final String USER_BALANCE_SQL =
  "SELECT * " +
  "FROM USERS_raj1174 " +
  "WHERE username = ? ";
  private PreparedStatement userBalanceStmt;

  private static final String UPDATE_USER_BALANCE_SQL =
  "UPDATE USERS_raj1174 " +
  "SET balance = ? " +
  "WHERE username = ? ";
  private PreparedStatement updateUserBalanceStmt;

  private static final String FIND_USER_RESERVATIONS_SQL =
  "SELECT * " +
  "FROM RESERVATIONS_raj1174 " +
  "WHERE username = ? ";
  private PreparedStatement userReservationsStmt;

  private static final String UPDATE_RESERVATION_PAID_SQL =
  "UPDATE RESERVATIONS_raj1174 " +
  "SET paid = ? " +
  "WHERE rid = ? ";
  private PreparedStatement updateReservationPaidStmt;

  private static final String FIND_TOTAL_CAPACITY =
  "SELECT COUNT(*) " +
  "FROM FLIGHTS_raj1174 f, RESERVATIONS_raj1174 r " +
  "WHERE f.fid = r.fid";

  //
  // Instance variables
  //
  private boolean LoggedIn;
  private String User;
  private List<Itinerary> itinerary;

  protected Query() throws SQLException, IOException {
    prepareStatements();
    this.LoggedIn = false;
    this.User = "";
    this.itinerary = new ArrayList<>();
  }

  /**
   * Clear the data in any custom tables created.
   *
   * WARNING! Do not drop any tables and do not clear the flights table.
   */
  public void clearTables() {
    try {
      tablesClearStmt.executeUpdate();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /*
   * prepare all the SQL statements in this method.
   */
  private void prepareStatements() throws SQLException {
    flightCapacityStmt = conn.prepareStatement(FLIGHT_CAPACITY_SQL);

    // Clear USERS table
    tablesClearStmt = conn.prepareStatement(CLEAR_TABLES_SQL);

    // Checks if the User exists
    userCheckStmt = conn.prepareStatement(CHECK_USER_SQL);

    // Creates a new User
    userCreateStmt = conn.prepareStatement(USER_CREATE_SQL);

    // Checks for Direct and Indirect flights
    searchDirectFlightStmt = conn.prepareStatement(SEARCH_DIRECT_SQL);
    searchIndirectFlightStmt = conn.prepareStatement(SEARCH_INDIRECT_SQL);

    // Checks the username with user that is logged in
    userReservationCheckStmt = conn.prepareStatement(CHECK_USER_RESERVATION_SQL);

    // Counts the number or flights
    reservationCountCheckStmt = conn.prepareStatement(RESERVATION_FLIGHT_COUNT_CHECK_SQL);

    // Counts the number of reservations
    numOfReservationsStmt = conn.prepareStatement(NUM_OF_RESERVATIONS_SQL);

    // Creates a new reservation
    createReservationStmt = conn.prepareStatement(CREATE_RESERVATION_SQL);

    // Gets all the users reservations
    findReservationStmt = conn.prepareStatement(FIND_USER_RESERVATION_SQL);

    // Gets the flights information
    findFlightStmt = conn.prepareStatement(FIND_FLIGHT_ID_SQL);

    // Gets the users balance
    userBalanceStmt = conn.prepareStatement(USER_BALANCE_SQL);

    // Updates the users balance
    updateUserBalanceStmt = conn.prepareStatement(UPDATE_USER_BALANCE_SQL);

    // Gets all of the users reservations
    userReservationsStmt = conn.prepareStatement(FIND_USER_RESERVATIONS_SQL);

    // Updates the reservation to paid if user pays it off
    updateReservationPaidStmt = conn.prepareStatement(UPDATE_RESERVATION_PAID_SQL);
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_login(String username, String password) {
    // Make sure username and password are not empty
    if (username == null || password == null){
      return "Login failed\n";
    }

    if (!LoggedIn) {

      try {

        conn.setAutoCommit(false);
        userCheckStmt.clearParameters();
        userCheckStmt.setString(1, username);
        ResultSet result = userCheckStmt.executeQuery();

        if (result.next()) {
          byte[] usersPassword = result.getBytes(2);
          if (PasswordUtils.plaintextMatchesSaltedHash(password, usersPassword)){
            LoggedIn = true;
            User = username;
            itinerary.clear();
            conn.commit();
            return "Logged in as " + username + "\n";
          }
        }

        result.close();
        conn.rollback();
        conn.setAutoCommit(true);
        return "Login failed\n";

      } catch (SQLException e){
        try {
          e.printStackTrace();
          conn.rollback();
          conn.setAutoCommit(true);
          if(isDeadlock(e)){
            return transaction_login(username, password);
          }
        } catch (Exception e1) {
          e1.printStackTrace();
        }
      }
    }

    return "User already logged in\n";
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_createCustomer(String username, String password, int initAmount) {
    if (username == null || password == null || username == "") {
      return "Failed to create user\n";
    }

    if (username.length() <= 20 && password.length() <= 20 && initAmount >= 0) {
      try {
        conn.setAutoCommit(false);
        userCheckStmt.setString(1, username);
        ResultSet result = userCheckStmt.executeQuery();

        if (!result.next()){
          userCreateStmt.setString(1, username);
          userCreateStmt.setBytes(2, PasswordUtils.saltAndHashPassword(password));
          userCreateStmt.setInt(3, initAmount);
          userCreateStmt.executeUpdate();
          conn.commit();
          return "Created user " + username + "\n";
        }

        result.close();
        conn.rollback();
        conn.setAutoCommit(true);

      } catch (SQLException e) {
        try {
          conn.rollback();
          conn.setAutoCommit(true);
          if(isDeadlock(e)){
            return transaction_createCustomer(username, password, initAmount);
          }
        } catch (SQLException e1) {
          e1.printStackTrace();
        }
      }
    }

    return "Failed to create user\n";
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_search(String originCity, String destinationCity,
                                   boolean directFlight, int dayOfMonth,
                                   int numberOfItineraries) {

    StringBuffer itineraryResult = new StringBuffer();

    try {
      itinerary.clear();
      searchDirectFlightStmt.clearParameters();
      searchDirectFlightStmt.setString(1, originCity);
      searchDirectFlightStmt.setString(2, destinationCity);
      searchDirectFlightStmt.setInt(3, dayOfMonth);

      ResultSet directFlightResult = searchDirectFlightStmt.executeQuery();
      while(directFlightResult.next() && itinerary.size() < numberOfItineraries) {
        Flight flight = new Flight(
          directFlightResult.getInt(directFlightResult.findColumn("fid")),
          directFlightResult.getInt(directFlightResult.findColumn("day_of_month")),
          directFlightResult.getString(directFlightResult.findColumn("carrier_id")),
          directFlightResult.getString(directFlightResult.findColumn("flight_num")),
          directFlightResult.getString(directFlightResult.findColumn("origin_city")),
          directFlightResult.getString(directFlightResult.findColumn("dest_city")),
          directFlightResult.getInt(directFlightResult.findColumn("actual_time")),
          directFlightResult.getInt(directFlightResult.findColumn("capacity")),
          directFlightResult.getInt(directFlightResult.findColumn("price"))
        );
        itinerary.add(new Itinerary(flight));
      }

      directFlightResult.close();

      if(!directFlight && itinerary.size() != numberOfItineraries) {
        searchIndirectFlightStmt.clearParameters();
        searchIndirectFlightStmt.setString(1, originCity);
        searchIndirectFlightStmt.setString(2, destinationCity);
        searchIndirectFlightStmt.setInt(3, dayOfMonth);
        searchIndirectFlightStmt.setInt(4, dayOfMonth);

        ResultSet indirectFlightsResult = searchIndirectFlightStmt.executeQuery();
        while(indirectFlightsResult.next() && itinerary.size() < numberOfItineraries) {
          Flight firstFlight = new Flight(
            indirectFlightsResult.getInt(indirectFlightsResult.findColumn("fid")),
            indirectFlightsResult.getInt(indirectFlightsResult.findColumn("day_of_month")),
            indirectFlightsResult.getString(indirectFlightsResult.findColumn("carrier_id")),
            indirectFlightsResult.getString(indirectFlightsResult.findColumn("flight_num")),
            indirectFlightsResult.getString(indirectFlightsResult.findColumn("origin_city")),
            indirectFlightsResult.getString(indirectFlightsResult.findColumn("dest_city")),
            indirectFlightsResult.getInt(indirectFlightsResult.findColumn("actual_time")),
            indirectFlightsResult.getInt(indirectFlightsResult.findColumn("capacity")),
            indirectFlightsResult.getInt(indirectFlightsResult.findColumn("price"))
          );

          Flight secondFlight = new Flight(
            indirectFlightsResult.getInt(indirectFlightsResult.findColumn("fid") + 18),
            indirectFlightsResult.getInt(indirectFlightsResult.findColumn("day_of_month") + 18),
            indirectFlightsResult.getString(indirectFlightsResult.findColumn("carrier_id") + 18),
            indirectFlightsResult.getString(indirectFlightsResult.findColumn("flight_num") + 18),
            indirectFlightsResult.getString(indirectFlightsResult.findColumn("origin_city") + 18),
            indirectFlightsResult.getString(indirectFlightsResult.findColumn("dest_city") + 18),
            indirectFlightsResult.getInt(indirectFlightsResult.findColumn("actual_time") + 18),
            indirectFlightsResult.getInt(indirectFlightsResult.findColumn("capacity") + 18),
            indirectFlightsResult.getInt(indirectFlightsResult.findColumn("price") + 18)
          );
          itinerary.add(new Itinerary(firstFlight, secondFlight));
        }

        indirectFlightsResult.close();
      }

      if(itinerary.isEmpty()) {
          return "No flights match your selection\n";
      }

      itinerary.sort(new ItineraryComparator());

      for(int i = 0; i < itinerary.size(); i++) {
        Itinerary result = itinerary.get(i);
        if(result.numFlights == 1) {
          itineraryResult.append("Itinerary " + i + ": 1 flight(s), " + result.totalTime + " minutes\n");
          itineraryResult.append(result.flight1.toString() + "\n");
        } else {
          itineraryResult.append("Itinerary " + i + ": 2 flight(s), " + result.totalTime + " minutes\n");
          itineraryResult.append(result.flight1.toString() + "\n");
          itineraryResult.append(result.flight2.toString() + "\n");
        }
      }

      conn.commit();
      conn.setAutoCommit(true);
      return itineraryResult.toString();

    } catch (SQLException e) {
      try {
        conn.rollback();
        conn.setAutoCommit(true);
        e.printStackTrace();
        return "Failed to search middle here\n";
      } catch (SQLException e1) {
        e1.printStackTrace();
      }
    }

    return "Failed to search out here\n";
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_book(int itineraryId) {
    if (!LoggedIn) {
      return "Cannot book reservations, not logged in\n";
    }

    if (itinerary.isEmpty() || itinerary.size() < itineraryId + 1) {
      return "No such itinerary " + itineraryId + "\n";
    }

    try {

      conn.setAutoCommit(false);
      userReservationCheckStmt.setString(1, User);
      ResultSet result = userReservationCheckStmt.executeQuery();

      while(result.next()) {
        int day = result.getInt("day_of_month");
        int itineraryDay = itinerary.get(itineraryId).flight1.dayOfMonth;
        if (day == itineraryDay) {
          conn.rollback();
          conn.setAutoCommit(true);
          return "You cannot book two flights in the same day\n";
        }
      }

      Flight firstFlight = itinerary.get(itineraryId).flight1;
      Flight secondFlight = itinerary.get(itineraryId).flight2;

      for(int i = 0; i < 2; i++) {
        Flight flight;

        if (i == 0) {
          flight = firstFlight;
        } else {
          flight = secondFlight;
        }

        if (flight != null) {
          reservationCountCheckStmt.clearParameters();
          reservationCountCheckStmt.setInt(1, flight.fid);
          reservationCountCheckStmt.setInt(2, flight.fid);
          result = reservationCountCheckStmt.executeQuery();
          result.next();

          int reservations = result.getInt(1);
          if(reservations == flight.capacity) {
            conn.rollback();
            conn.setAutoCommit(true);
            return "Booking failed\n";
          }
        }
      }

      numOfReservationsStmt.clearParameters();
      result = numOfReservationsStmt.executeQuery();
      result.next();

      int numReservations = result.getInt(1);
      result.close();

      createReservationStmt.setString(1, User);
      createReservationStmt.setInt(2, numReservations + 1);
      createReservationStmt.setInt(3, firstFlight.fid);

      if (secondFlight != null) {
        createReservationStmt.setInt(4, secondFlight.fid);
      } else {
        createReservationStmt.setObject(4, null);
      }

      createReservationStmt.setInt(5, 0);
      createReservationStmt.executeUpdate();
      conn.commit();
      conn.setAutoCommit(true);
      return "Booked flight(s), reservation ID: " + (numReservations + 1) + "\n";

    } catch (SQLException e) {
      try {
        conn.rollback();
        conn.setAutoCommit(true);
        if (isDeadlock(e)) {
          return transaction_book(itineraryId);
        } else {
          e.printStackTrace();
          return "Booking failed\n";
        }
      } catch (SQLException e1) {
        e1.printStackTrace();
        return "Booking failed\n";
      }
    }
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_pay(int reservationId) {
    if (!LoggedIn) {
      return "Cannot pay, not logged in\n";
    }

    try {

      conn.setAutoCommit(false);
      findReservationStmt.clearParameters();
      findReservationStmt.setString(1, User);
      findReservationStmt.setInt(2, reservationId);
      ResultSet result = findReservationStmt.executeQuery();

      if(!result.next()) {
        conn.rollback();
        conn.setAutoCommit(true);
        return "Cannot find unpaid reservation " + reservationId + " under user: " + User + "\n";
      }

      int paid = result.getInt("paid");
      if (paid > 0) {
        conn.rollback();
        conn.setAutoCommit(true);
        return "Cannot find unpaid reservation " + reservationId + " under user: " + User + "\n";
      }

      int flight1 = result.getInt(3);
      int flight2 = -1;
      Object flight2IndexObject = result.getObject(4);
      if (flight2IndexObject != null) {
        flight2 = result.getInt(4);
      }

      int totalPrice = 0;
      for (int i = 0; i < 2; i++) {
        int fid;
        if (i == 0){
          fid = flight1;
        } else {
          fid = flight2;
        }

        if (fid != -1) {
          findFlightStmt.clearParameters();
          findFlightStmt.setInt(1, fid);
          ResultSet resultSet = findFlightStmt.executeQuery();
          resultSet.next();
          int price = resultSet.getInt(resultSet.findColumn("price"));
          totalPrice = totalPrice + price;
        }
      }

      userBalanceStmt.clearParameters();
      userBalanceStmt.setString(1, User);
      result = userBalanceStmt.executeQuery();
      result.next();

      int userBalance = result.getInt(3);
      result.close();

      if(userBalance < totalPrice) {
        conn.rollback();
        conn.setAutoCommit(true);
        return "User has only " + userBalance + " in account but itinerary costs " + totalPrice + "\n";
      }

      updateUserBalanceStmt.clearParameters();
      updateUserBalanceStmt.setInt(1, userBalance - totalPrice);
      updateUserBalanceStmt.setString(2, User);
      updateUserBalanceStmt.executeUpdate();
      updateReservationPaidStmt.clearParameters();
      updateReservationPaidStmt.setInt(1, 1);
      updateReservationPaidStmt.setInt(2, reservationId);
      updateReservationPaidStmt.executeUpdate();

      conn.commit();
      conn.setAutoCommit(true);
      return "Paid reservation: " + reservationId + " remaining balance: " + (userBalance - totalPrice) + "\n";

    } catch(SQLException e){
      try {
        conn.rollback();
        conn.setAutoCommit(true);
        e.printStackTrace();
        if(isDeadlock(e)) {
          return transaction_pay(reservationId);
        }
      } catch (SQLException e1) {
        e1.printStackTrace();
      }
    }

    return "Failed to pay for reservation " + reservationId + "\n";
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_reservations() {
    if (!LoggedIn) {
      return "Cannot view reservations, not logged in\n";
    }

    try {

      StringBuffer reservationsResult = new StringBuffer();
      userReservationsStmt.clearParameters();
      userReservationsStmt.setString(1, User);
      ResultSet result = userReservationsStmt.executeQuery();

      ResultSet flights;
      while(result.next()) {
        String paid;
        if (result.getInt("paid") > 0) {
          paid = "true";
        } else {
          paid = "false";
        }

        reservationsResult.append("Reservation " + result.getInt("rid") + " paid: " + paid + ":\n");

        for(int i = 0; i < 2; i++) {
          String fid;
          if (i == 0) {
            fid = "fid";
          } else {
            fid = "fid2";
          }

          findFlightStmt.clearParameters();
          findFlightStmt.setInt(1, result.getInt(fid));
          flights = findFlightStmt.executeQuery();

          if(flights.next()) {
            Flight flight = new Flight(
              flights.getInt("fid"),
              flights.getInt("day_of_month"),
              flights.getString("carrier_id"),
              flights.getString("flight_num"),
              flights.getString("origin_city"),
              flights.getString("dest_city"),
              flights.getInt("actual_time"),
              flights.getInt("capacity"),
              flights.getInt("price")
            );

            reservationsResult.append(flight.toString() + "\n");
          }
        }
      }

      conn.commit();
      conn.setAutoCommit(true);
      return reservationsResult.toString();

    } catch(SQLException e){
      try {
        conn.rollback();
        conn.setAutoCommit(true);
        e.printStackTrace();
        if(isDeadlock(e)) {
          return transaction_reservations();
        }
      } catch (SQLException e1) {
        e1.printStackTrace();
      }
    }

    return "Failed to retrieve reservations\n";
  }

  /**
   * Example utility function that uses prepared statements
   */
  private int checkFlightCapacity(int fid) throws SQLException {
    flightCapacityStmt.clearParameters();
    flightCapacityStmt.setInt(1, fid);

    ResultSet results = flightCapacityStmt.executeQuery();
    results.next();
    int capacity = results.getInt("capacity");
    results.close();

    return capacity;
  }

  /**
   * Utility function to determine whether an error was caused by a deadlock
   */
  private static boolean isDeadlock(SQLException e) {
    return e.getErrorCode() == 1205;
  }

  /**
   * A class to store information about a single flight
   *
   * TODO(hctang): move this into QueryAbstract
   */
  class Flight {
    public int fid;
    public int dayOfMonth;
    public String carrierId;
    public String flightNum;
    public String originCity;
    public String destCity;
    public int time;
    public int capacity;
    public int price;

    Flight(int id, int day, String carrier, String fnum, String origin, String dest, int tm,
           int cap, int pri) {
      fid = id;
      dayOfMonth = day;
      carrierId = carrier;
      flightNum = fnum;
      originCity = origin;
      destCity = dest;
      time = tm;
      capacity = cap;
      price = pri;
    }

    @Override
    public String toString() {
      return "ID: " + fid + " Day: " + dayOfMonth + " Carrier: " + carrierId + " Number: "
          + flightNum + " Origin: " + originCity + " Dest: " + destCity + " Duration: " + time
          + " Capacity: " + capacity + " Price: " + price;
    }
  }

  // New Itinerary class that defines the Itinerary
  private class Itinerary {
    Flight flight1;
    Flight flight2;
    int totalTime;
    int numFlights;

    public Itinerary (Flight flight1) {
      this.flight1 = flight1;
      this.totalTime = flight1.time;
      this.numFlights = 1;
    }

    public Itinerary (Flight flight1, Flight flight2) {
      this.flight1 = flight1;
      this.flight2 = flight2;
      this.totalTime = flight1.time + flight2.time;
      this.numFlights = 2;
    }
  }

  // New ItineraryComparator class that compares Itinerarys
  public class ItineraryComparator implements Comparator<Itinerary> {
    @Override
    public int compare(Itinerary itinerary, Itinerary itinerary2) {
      return Integer.compare(itinerary.totalTime, itinerary2.totalTime);
    }
  }
}
