export interface TokenData {
    key: string;
    secret: string;
    username: string;
    mcUUID: string;
}

// oauth -> creates the key -> stores that info into MineTogether -> sends to app backend

export interface AuthState {
    token: TokenData | null;
    error: boolean;
    loading: boolean;
}
