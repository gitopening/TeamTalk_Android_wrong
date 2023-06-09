#include "SSLSocket.h"
#include "EventDispatch.h"

SSL_CTX *m_ssl_context;

SSL_CTX* InitServerCTX(void)
{   
	const SSL_METHOD *method;
	SSL_CTX *ctx;
	SSL_library_init();
	OpenSSL_add_all_algorithms();	   /* load & register all cryptos, etc. */
	SSL_load_error_strings();		   /* load all error messages */
	method = SSLv23_server_method();	 /* create new server-method instance */
	ctx = SSL_CTX_new(method);		  /* create new context from method */
	if ( ctx == NULL )
	{
		ERR_print_errors_fp(stderr);
		abort();
	}
	return ctx;
}

void LoadCertificates(SSL_CTX* ctx, char* CertFile, char* KeyFile)
{
	/* set the local certificate from CertFile */
	SSL_CTX_set_ecdh_auto(ctx, 1);
	if ( SSL_CTX_use_certificate_file(ctx, CertFile, SSL_FILETYPE_PEM) <= 0 )
	{
		ERR_print_errors_fp(stderr);
		abort();
	}
	/* set the private key from KeyFile (may be the same as CertFile) */
	if ( SSL_CTX_use_PrivateKey_file(ctx, KeyFile, SSL_FILETYPE_PEM) <= 0 )
	{
		ERR_print_errors_fp(stderr);
		abort();
	}
	/* verify private key */
	if ( !SSL_CTX_check_private_key(ctx) )
	{
		fprintf(stderr, "Private key does not match the public certificate\n");
		abort();
	}
}

void CSSLSocket::_AcceptNewSocket()
{
	SOCKET fd = 0;
	sockaddr_in peer_addr;
	socklen_t addr_len = sizeof(sockaddr_in);
	char ip_str[64];
	while ( (fd = accept(GetSocket(), (sockaddr*)&peer_addr, &addr_len)) != INVALID_SOCKET )
	{
		log("new client");
		SSL *ssl = SSL_new(m_ssl_context);
		SSL_set_fd(ssl, fd);
		if(SSL_accept(ssl)  == -1) {
			close(fd);
			SSL_free(ssl);
			log("auth failed");
			continue;
		}
		CSSLSocket* pSocket = new CSSLSocket();
		pSocket->setSSL(ssl);
		uint32_t ip = ntohl(peer_addr.sin_addr.s_addr);
		uint16_t port = ntohs(peer_addr.sin_port);
		snprintf(ip_str, sizeof(ip_str), "%d.%d.%d.%d", ip >> 24, (ip >> 16) & 0xFF, (ip >> 8) & 0xFF, ip & 0xFF);

		log("AcceptNewSocket, socket=%d from %s:%d\n", fd, ip_str, port);
		pSocket->SetSocket(fd);
		pSocket->SetCallback(m_callback);
		pSocket->SetCallbackData(m_callback_data);
		pSocket->SetState(SOCKET_STATE_CONNECTED);
		pSocket->SetRemoteIP(ip_str);
		pSocket->SetRemotePort(port);
		_SetNoDelay(fd);
		_SetNonblock(fd);
		BindEvent(pSocket,SOCKET_READ | SOCKET_EXCEP);
		if(m_callback != NULL){
			m_callback(m_callback_data, NETLIB_MSG_CONNECT, (net_handle_t)fd, pSocket);
		}
	}
}
int CSSLSocket::Recv(void* buf, int len)
{
	return  SSL_read(m_ssl, buf, len);
}

int CSSLSocket::Send(void* buf, int len)
{
	if (GetState() != SOCKET_STATE_CONNECTED)
		return NETLIB_ERROR;
	int offset = 0;
	int send_size = 4096;
	int main = len;
	while(main > 0) {
		if(main < send_size) {
			send_size = main;
		}
		//int ret = BIO_write(buf_io, buf+offset , send_size); 
		int ret = SSL_write(m_ssl, (char*)buf+offset , send_size);
		if(ret <=0) {
			int err_code = SSL_get_error(m_ssl, ret); 
			if(SSL_ERROR_WANT_WRITE == err_code) {
			continue;
			}else {
#if (defined _WIN32)
				CEventDispatch::Instance()->AddEvent(GetSocket(), SOCKET_WRITE);
#elif (defined __APPLE__)
				CEventInterface *event = FindEvent(GetSocket());
				if(event != NULL){
					CEventDispatch::Instance()->AddEventInterface(GetSocket(), SOCKET_WRITE,this);
				}
#endif
			}
			log("write ok for:%d",len - main);
			break;
		}		  
		main -= ret;
		offset += ret;
	}

	if(main < len) {
		return len - main;
	}
	return 0;
}

void initSSLConfig(char* CertFile, char* KeyFile)
{
	m_ssl_context = InitServerCTX();
	if(m_ssl_context != NULL) {
		LoadCertificates(m_ssl_context, CertFile , KeyFile);	
	}
}


