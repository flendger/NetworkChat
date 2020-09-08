package messages;

public enum MessageType {
    AUTH {
        @Override
        public String toString() {
            return "/auth";
        }
    },
    AUTH_OK {
        @Override
        public String toString() {
            return "/authok";
        }
    },
    AUTH_FAILED {
        @Override
        public String toString() {
            return "/authfailed";
        }
    },
    LOGIN {
        @Override
        public String toString() {
            return "/login";
        }
    },
    LOGOFF {
        @Override
        public String toString() {
            return "/logoff";
        }
    },
    EXIT {
        @Override
        public String toString() {
            return "/exit";
        }
    },
    GET_USERS {
        @Override
        public String toString() {
            return "/getusers";
        }
    },
    USERS {
        @Override
        public String toString() {
            return "/users";
        }
    },
    MSG {
        @Override
        public String toString() {
            return "/msg";
        }
    },
    CHGNAME {
        @Override
        public String toString() {
            return "/chgname";
        }
    },
    CHGNAMEOK {
        @Override
        public String toString() {
            return "/chgnameok";
        }
    },
    PRVMSG {
        @Override
        public String toString() {
            return "/prvmsg";
        }
    },
    NONE {
        @Override
        public String toString() {
            return "";
        }
    };

    public static MessageType getType(String s) {
        switch (s) {
            case "/auth":
                return AUTH;
            case "/authok":
                return AUTH_OK;
            case "/authfailed":
                return AUTH_FAILED;
            case "/login":
                return LOGIN;
            case "/logoff":
                return LOGOFF;
            case "/exit":
                return EXIT;
            case "/getusers":
                return GET_USERS;
            case "/users":
                return USERS;
            case "/msg":
                return MSG;
            case "/chgname":
                return CHGNAME;
            case "/chgnameok":
                return CHGNAMEOK;
            case "/prvmsg":
                return PRVMSG;
            default:
                return NONE;
        }
    }
}
